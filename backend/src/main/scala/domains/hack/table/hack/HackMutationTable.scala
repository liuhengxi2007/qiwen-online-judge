package domains.hack.table.hack

import cats.effect.IO
import domains.hack.objects.{HackId, HackStatus}
import domains.problem.objects.{ProblemId, ProblemSlug}
import domains.submission.objects.SubmissionId
import domains.user.objects.Username
import domains.hack.table.hack.HackTableSupport.*
import judgeprotocol.objects.response.JudgeResult

import java.sql.{Connection, Timestamp}
import java.time.Instant
import java.util.UUID

/** hack_attempts 表写入入口；负责创建 attempt 和记录 worker 完成结果。 */
object HackMutationTable:

  private val insertAttemptSQL: String =
    """
      |insert into hack_attempts (
      |  id, public_id, problem_id, target_submission_public_id, author_username,
      |  subtask_index, subtask_label, status, input_text, strategy_provider_source,
      |  old_score, created_at, started_at, finished_at
      |)
      |values (?, nextval('hack_public_id_seq'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, null, null)
      |returning public_id
      |""".stripMargin

  /** 插入 queued hack attempt 并返回公开 id。 */
  def insertAttempt(
    connection: Connection,
    id: UUID,
    problemId: ProblemId,
    targetSubmissionId: SubmissionId,
    authorUsername: Username,
    subtaskIndex: Int,
    subtaskLabel: Option[String],
    input: String,
    strategyProviderSource: Option[String],
    oldScore: BigDecimal,
    createdAt: Instant
  ): IO[HackId] =
    IO.blocking {
      val statement = connection.prepareStatement(insertAttemptSQL)
      try
        statement.setObject(1, id)
        statement.setObject(2, problemId.value)
        statement.setLong(3, targetSubmissionId.value)
        statement.setString(4, authorUsername.value)
        statement.setInt(5, subtaskIndex)
        subtaskLabel match
          case Some(value) => statement.setString(6, value)
          case None => statement.setNull(6, java.sql.Types.VARCHAR)
        statement.setString(7, encodeHackStatusColumn(HackStatus.Queued))
        statement.setString(8, input)
        strategyProviderSource match
          case Some(value) => statement.setString(9, value)
          case None => statement.setNull(9, java.sql.Types.VARCHAR)
        statement.setBigDecimal(10, oldScore.bigDecimal)
        statement.setTimestamp(11, Timestamp.from(createdAt))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then HackId(resultSet.getLong("public_id"))
          else throw IllegalStateException("Insert succeeded but returned no hack attempt.")
        finally resultSet.close()
      finally statement.close()
    }

  private val completeAttemptSQL: String =
    """
      |update hack_attempts h
      |set status = ?,
      |    answer_text = ?,
      |    judge_result = ?::jsonb,
      |    validator_message = ?,
      |    standard_message = ?,
      |    target_message = ?,
      |    finished_at = ?
      |from problems p
      |where h.public_id = ?
      |  and h.status = 'running'
      |  and p.id = h.problem_id
      |returning h.problem_id, p.slug as problem_slug, h.subtask_index, h.input_text, h.author_username
      |""".stripMargin

  /** 完成成功 hack 后用于物化题目数据的来源信息。 */
  final case class CompletedAttemptSource(
    problemId: ProblemId,
    problemSlug: ProblemSlug,
    subtaskIndex: Int,
    input: String,
    authorUsername: Username
  )

  /** 仅更新 running hack 的终态结果；返回物化所需来源信息，未命中 running 记录时返回 None。 */
  def completeAttempt(
    connection: Connection,
    hackId: HackId,
    status: HackStatus,
    answer: Option[String],
    newResult: Option[JudgeResult],
    validatorMessage: Option[String],
    standardMessage: Option[String],
    targetMessage: Option[String],
    finishedAt: Instant
  ): IO[Option[CompletedAttemptSource]] =
    IO.blocking {
      val statement = connection.prepareStatement(completeAttemptSQL)
      try
        statement.setString(1, encodeHackStatusColumn(status))
        answer match
          case Some(value) => statement.setString(2, value)
          case None => statement.setNull(2, java.sql.Types.VARCHAR)
        setOptionalJudgeResult(statement, 3, newResult)
        validatorMessage match
          case Some(value) => statement.setString(4, value)
          case None => statement.setNull(4, java.sql.Types.VARCHAR)
        standardMessage match
          case Some(value) => statement.setString(5, value)
          case None => statement.setNull(5, java.sql.Types.VARCHAR)
        targetMessage match
          case Some(value) => statement.setString(6, value)
          case None => statement.setNull(6, java.sql.Types.VARCHAR)
        statement.setTimestamp(7, Timestamp.from(finishedAt))
        statement.setLong(8, hackId.value)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            Some(
              CompletedAttemptSource(
                problemId = ProblemId(resultSet.getObject("problem_id", classOf[UUID])),
                problemSlug = ProblemSlug(resultSet.getString("problem_slug")),
                subtaskIndex = resultSet.getInt("subtask_index"),
                input = resultSet.getString("input_text"),
                authorUsername = Username.canonical(resultSet.getString("author_username"))
              )
            )
          else None
        finally resultSet.close()
      finally statement.close()
    }
