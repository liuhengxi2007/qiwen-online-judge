package domains.submission.table.submission

import cats.effect.IO
import domains.problem.objects.{ProblemId, ProblemSlug}
import domains.submission.objects.{SubmissionId, SubmissionLanguage}
import domains.submission.objects.internal.{ClaimedSubmission, SubmissionJudgeState}
import domains.submission.table.submission.SubmissionTableSupport.*

import java.sql.{Connection, Timestamp}
import java.time.Instant

/** submissions 表判题队列写入入口；负责 claim、手动重判、hack 重判和状态更新。 */
object SubmissionJudgeTable:

  val OrdinaryPriority: Int = 0
  val ManualProblemRejudgePriority: Int = -50
  val LowPriority: Int = -100

  private def claimNextForLanguagesSQL(languageCount: Int): String =
    val placeholders = List.fill(languageCount)("?").mkString(", ")
    val languagePredicate =
      if languageCount == 0 then "program.data ->> 'language' = 'text'"
      else s"(program.data ->> 'language' = 'text' or program.data ->> 'language' in ($placeholders))"
    s"""
      |with next_submission as (
      |  select s.id
      |  from submissions s
      |  join problems p on p.id = s.problem_id
      |  where s.status = 'queued'
      |    and s.judge_priority >= ?
      |    and exists (
      |      select 1
      |      from jsonb_each(s.program_manifest -> 'programs') as program(role, data)
      |      where $languagePredicate
      |    )
      |    and p.ready = true
      |  order by s.judge_priority desc, s.judge_queued_at asc, s.public_id asc
      |  for update skip locked
      |  limit 1
      |)
      |update submissions s
      |set status = ?,
      |    started_at = ?,
      |    finished_at = ?,
      |    rejudge_revision = p.rejudge_revision,
      |    judge_result = null
      |from next_submission ns, problems p
      |where s.id = ns.id
      |  and p.id = s.problem_id
      |returning s.public_id, s.problem_id, p.slug as problem_slug, s.program_manifest::text as program_manifest
      |""".stripMargin

  /** 原子领取下一个 queued 提交；使用 skip locked 支持多个 worker 并发 claim。 */
  def claimNextForLanguages(
    connection: Connection,
    languages: List[SubmissionLanguage],
    runningState: SubmissionJudgeState,
    minPriority: Int
  ): IO[Option[ClaimedSubmission]] =
    IO.blocking {
      val statement = connection.prepareStatement(claimNextForLanguagesSQL(languages.size))
      try
        statement.setInt(1, minPriority)
        languages.zipWithIndex.foreach { case (language, index) =>
          statement.setString(index + 2, encodeSubmissionLanguageColumn(language))
        }
        val stateStartIndex = languages.size + 2
        statement.setString(stateStartIndex, encodeSubmissionStatusColumn(runningState.status))
        setOptionalTimestamp(statement, stateStartIndex + 1, runningState.startedAt)
        setOptionalTimestamp(statement, stateStartIndex + 2, runningState.finishedAt)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            Some(
              ClaimedSubmission(
                id = SubmissionId(resultSet.getLong("public_id")),
                problemId = ProblemId(resultSet.getObject("problem_id", classOf[java.util.UUID])),
                problemSlug = parseColumn("submissions.problem_slug", resultSet.getString("problem_slug"), ProblemSlug.parse),
                programManifest = readProgramManifest(resultSet, "program_manifest")
              )
            )
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val queueManualRejudgeSQL: String =
    """
      |update submissions
      |set status = 'queued',
      |    judge_result = null,
      |    started_at = null,
      |    finished_at = null,
      |    judge_priority = ?,
      |    judge_queued_at = ?
      |where public_id = ?
      |  and status in ('completed', 'failed')
      |""".stripMargin

  /** 将单个终态提交重新入队为普通优先级；调用方负责先检查权限和当前状态。 */
  def queueManualRejudge(connection: Connection, submissionId: SubmissionId): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(queueManualRejudgeSQL)
      try
        statement.setInt(1, OrdinaryPriority)
        statement.setTimestamp(2, nowTimestamp)
        statement.setLong(3, submissionId.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }

  private val queueHackRejudgeForProblemSQL: String =
    """
      |update submissions
      |set status = 'queued',
      |    judge_result = null,
      |    started_at = null,
      |    finished_at = null,
      |    judge_priority = ?,
      |    judge_queued_at = ?
      |where problem_id = ?
      |  and status in ('completed', 'failed')
      |""".stripMargin

  /** 将题目下已终态提交低优先级重新入队；用于成功 hack 后批量重判。 */
  def queueHackRejudgeForProblem(connection: Connection, problemId: ProblemId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(queueHackRejudgeForProblemSQL)
      try
        statement.setInt(1, LowPriority)
        statement.setTimestamp(2, nowTimestamp)
        statement.setObject(3, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val queueManualRejudgeForProblemSQL: String =
    """
      |update submissions
      |set status = 'queued',
      |    judge_result = null,
      |    started_at = null,
      |    finished_at = null,
      |    judge_priority = ?,
      |    judge_queued_at = ?
      |where problem_id = ?
      |  and (
      |    status in ('completed', 'failed')
      |    or (status = 'queued' and judge_priority < ?)
      |  )
      |""".stripMargin

  /** 将题目下已终态提交入队，并提升优先级低于手动整题重判的已排队提交。 */
  def queueManualRejudgeForProblem(connection: Connection, problemId: ProblemId): IO[Int] =
    IO.blocking {
      val statement = connection.prepareStatement(queueManualRejudgeForProblemSQL)
      try
        statement.setInt(1, ManualProblemRejudgePriority)
        statement.setTimestamp(2, nowTimestamp)
        statement.setObject(3, problemId.value)
        statement.setInt(4, ManualProblemRejudgePriority)
        statement.executeUpdate()
      finally statement.close()
    }

  private val requeueIfRejudgeRevisionStaleSQL: String =
    """
      |update submissions s
      |set status = 'queued',
      |    judge_result = null,
      |    started_at = null,
      |    finished_at = null,
      |    judge_priority = ?,
      |    judge_queued_at = ?
      |from problems p
      |where s.public_id = ?
      |  and p.id = s.problem_id
      |  and s.status in ('completed', 'failed')
      |  and s.rejudge_revision < p.rejudge_revision
      |""".stripMargin

  /** 若提交的重判 revision 落后于题目则低优先级重新入队，返回是否更新。 */
  def requeueIfRejudgeRevisionStale(connection: Connection, submissionId: SubmissionId): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(requeueIfRejudgeRevisionStaleSQL)
      try
        statement.setInt(1, LowPriority)
        statement.setTimestamp(2, nowTimestamp)
        statement.setLong(3, submissionId.value)
        statement.executeUpdate() > 0
      finally statement.close()
    }

  private def nowTimestamp: Timestamp =
    Timestamp.from(Instant.now())

  private val updateJudgeStateSQL: String =
    """
      |update submissions
      |set status = ?, judge_result = ?::jsonb, started_at = ?, finished_at = ?
      |where public_id = ?
      |  and status = 'running'
      |  and started_at is not distinct from ?
      |""".stripMargin

  /** 写入提交判题状态快照；生命周期合法性由 SubmissionJudgeRules 在调用前校验。 */
  def updateJudgeState(
    connection: Connection,
    submissionId: SubmissionId,
    judgeState: SubmissionJudgeState
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(updateJudgeStateSQL)
      try
        statement.setString(1, encodeSubmissionStatusColumn(judgeState.status))
        setOptionalJudgeResult(statement, 2, judgeState.judgeResult)
        setOptionalTimestamp(statement, 3, judgeState.startedAt)
        setOptionalTimestamp(statement, 4, judgeState.finishedAt)
        statement.setLong(5, submissionId.value)
        setOptionalTimestamp(statement, 6, judgeState.startedAt)
        statement.executeUpdate() > 0
      finally statement.close()
    }
