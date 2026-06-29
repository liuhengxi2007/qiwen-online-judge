package domains.hack.table.hack

import cats.effect.IO
import domains.hack.objects.HackId
import domains.problem.objects.{ProblemId, ProblemSlug}
import domains.submission.objects.{SubmissionId, SubmissionLanguage}
import domains.submission.objects.internal.{ClaimedSubmission, SubmissionProgramManifest}
import domains.user.objects.Username
import domains.hack.table.hack.HackTableSupport.*
import io.circe.parser.decode
import judgeprotocol.objects.response.JudgeResult

import java.sql.{Connection, ResultSet, Timestamp}
import java.time.Instant
import java.util.UUID

/** hack_attempts 表的 worker claim 入口；负责并发领取可执行 hack 并读取目标提交上下文。 */
object HackJudgeTable:

  /** claim 后供 worker 构建 HackTask 的数据库记录。 */
  final case class ClaimedHackAttemptRecord(
    hackId: HackId,
    targetSubmission: ClaimedSubmission,
    authorUsername: Username,
    subtaskIndex: Int,
    input: String,
    strategyProviderSource: Option[String],
    oldResult: JudgeResult
  )

  private def claimNextSQL(languageCount: Int): String =
    val placeholders = List.fill(languageCount)("?").mkString(", ")
    val languagePredicate =
      if languageCount == 0 then "program.data ->> 'language' = 'text'"
      else s"(program.data ->> 'language' = 'text' or program.data ->> 'language' in ($placeholders))"
    s"""
      |with next_hack as (
      |  select h.id
      |  from hack_attempts h
      |  join submissions target on target.public_id = h.target_submission_public_id
      |  join problems p on p.id = h.problem_id
      |  where h.status = 'queued'
      |    and target.status = 'completed'
      |    and target.judge_result is not null
      |    and p.ready = true
      |    and exists (
      |      select 1
      |      from jsonb_each(target.program_manifest -> 'programs') as program(role, data)
      |      where $languagePredicate
      |    )
      |  order by h.created_at asc, h.public_id asc
      |  for update skip locked
      |  limit 1
      |)
      |update hack_attempts h
      |set status = 'running',
      |    started_at = ?
      |from next_hack nh
      |join submissions target on true
      |join problems p on true
      |where h.id = nh.id
      |  and target.public_id = h.target_submission_public_id
      |  and p.id = h.problem_id
      |returning h.public_id,
      |          h.author_username,
      |          h.subtask_index,
      |          h.input_text,
      |          h.strategy_provider_source,
      |          target.public_id as target_submission_public_id,
      |          target.problem_id as target_problem_id,
      |          p.slug as problem_slug,
      |          target.program_manifest::text as program_manifest,
      |          target.judge_result::text as old_result
      |""".stripMargin

  /** 原子领取一个 queued hack；使用 skip locked 支持多个 worker 并发 claim。 */
  def claimNextForLanguages(
    connection: Connection,
    languages: List[SubmissionLanguage],
    startedAt: Instant
  ): IO[Option[ClaimedHackAttemptRecord]] =
    IO.blocking {
      val statement = connection.prepareStatement(claimNextSQL(languages.size))
      try
        languages.zipWithIndex.foreach { case (language, index) =>
          statement.setString(index + 1, encodeSubmissionLanguage(language))
        }
        statement.setTimestamp(languages.size + 1, Timestamp.from(startedAt))
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readClaimedHackAttempt(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }

  private def readClaimedHackAttempt(resultSet: ResultSet): ClaimedHackAttemptRecord =
    val targetSubmission =
      ClaimedSubmission(
        id = SubmissionId(resultSet.getLong("target_submission_public_id")),
        problemId = ProblemId(resultSet.getObject("target_problem_id", classOf[UUID])),
        problemSlug = ProblemSlug(resultSet.getString("problem_slug")),
        programManifest = readProgramManifest(resultSet.getString("program_manifest"))
      )
    ClaimedHackAttemptRecord(
      hackId = HackId(resultSet.getLong("public_id")),
      targetSubmission = targetSubmission,
      authorUsername = Username.canonical(resultSet.getString("author_username")),
      subtaskIndex = resultSet.getInt("subtask_index"),
      input = resultSet.getString("input_text"),
      strategyProviderSource = Option(resultSet.getString("strategy_provider_source")),
      oldResult = decode[JudgeResult](resultSet.getString("old_result"))
        .fold(error => throw IllegalStateException(s"Invalid target judge result JSON: ${error.getMessage}"), identity)
    )

  private def readProgramManifest(raw: String): SubmissionProgramManifest =
    decode[SubmissionProgramManifest](raw)
      .fold(error => throw IllegalStateException(s"Invalid submission program manifest JSON: ${error.getMessage}"), identity)

  private def encodeSubmissionLanguage(value: SubmissionLanguage): String =
    value match
      case SubmissionLanguage.Cpp17 => "cpp17"
      case SubmissionLanguage.Python3 => "python3"
      case SubmissionLanguage.Text => "text"
