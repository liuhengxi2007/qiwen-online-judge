package domains.submission.table

import domains.auth.table.UserIdentityTableSupport.readUserIdentity
import domains.problem.model.{ProblemId, ProblemSlug, ProblemTitle}
import domains.submission.model.{SubmissionDetail, SubmissionId, SubmissionJudgeState, SubmissionLanguage, SubmissionSourceCode, SubmissionStatus, SubmissionSummary, SubmissionVerdict}

import java.sql.{PreparedStatement, ResultSet, Timestamp}
import java.time.Instant

object SubmissionTableSupport:

  def readSubmissionSummary(resultSet: ResultSet): SubmissionSummary =
    SubmissionSummary(
      id = SubmissionId(resultSet.getLong("public_id")),
      problemId = ProblemId(resultSet.getObject("problem_id", classOf[java.util.UUID])),
      problemSlug = parseColumn("submissions.problem_slug", resultSet.getString("problem_slug"), ProblemSlug.parse),
      problemTitle = parseColumn("submissions.problem_title", resultSet.getString("problem_title"), ProblemTitle.parse),
      submitter = readUserIdentity(resultSet, "submitter"),
      language = parseColumn("submissions.language", resultSet.getString("language"), SubmissionLanguage.parse),
      status = parseColumn("submissions.status", resultSet.getString("status"), SubmissionStatus.parse),
      verdict = Option(resultSet.getString("verdict")).flatMap(SubmissionVerdict.fromDatabase),
      timeUsedMs = readOptionalLong(resultSet, "time_used_ms"),
      memoryUsedKb = readOptionalLong(resultSet, "memory_used_kb"),
      codeLength = resultSet.getInt("code_length"),
      submittedAt = resultSet.getTimestamp("submitted_at").toInstant,
      startedAt = Option(resultSet.getTimestamp("started_at")).map(_.toInstant),
      finishedAt = Option(resultSet.getTimestamp("finished_at")).map(_.toInstant)
    )

  def readSubmissionDetail(resultSet: ResultSet): SubmissionDetail =
    SubmissionDetail(
      id = SubmissionId(resultSet.getLong("public_id")),
      problemId = ProblemId(resultSet.getObject("problem_id", classOf[java.util.UUID])),
      problemSlug = parseColumn("submissions.problem_slug", resultSet.getString("problem_slug"), ProblemSlug.parse),
      problemTitle = parseColumn("submissions.problem_title", resultSet.getString("problem_title"), ProblemTitle.parse),
      canManage = false,
      submitter = readUserIdentity(resultSet, "submitter"),
      language = parseColumn("submissions.language", resultSet.getString("language"), SubmissionLanguage.parse),
      status = parseColumn("submissions.status", resultSet.getString("status"), SubmissionStatus.parse),
      verdict = Option(resultSet.getString("verdict")).flatMap(SubmissionVerdict.fromDatabase),
      judgeMessage = Option(resultSet.getString("judge_message")),
      timeUsedMs = readOptionalLong(resultSet, "time_used_ms"),
      memoryUsedKb = readOptionalLong(resultSet, "memory_used_kb"),
      codeLength = resultSet.getInt("code_length"),
      sourceCode = parseColumn("submissions.source_code", resultSet.getString("source_code"), SubmissionSourceCode.parse),
      submittedAt = resultSet.getTimestamp("submitted_at").toInstant,
      startedAt = Option(resultSet.getTimestamp("started_at")).map(_.toInstant),
      finishedAt = Option(resultSet.getTimestamp("finished_at")).map(_.toInstant)
    )

  def setOptionalVerdict(
    statement: PreparedStatement,
    parameterIndex: Int,
    verdict: Option[SubmissionVerdict]
  ): Unit =
    verdict match
      case Some(value) => statement.setString(parameterIndex, SubmissionVerdict.toDatabase(value))
      case None => statement.setNull(parameterIndex, java.sql.Types.VARCHAR)

  def setOptionalJudgeMessage(
    statement: PreparedStatement,
    parameterIndex: Int,
    judgeMessage: Option[String]
  ): Unit =
    judgeMessage match
      case Some(value) => statement.setString(parameterIndex, value)
      case None => statement.setNull(parameterIndex, java.sql.Types.LONGVARCHAR)

  def setOptionalLong(
    statement: PreparedStatement,
    parameterIndex: Int,
    value: Option[Long]
  ): Unit =
    value match
      case Some(currentValue) => statement.setLong(parameterIndex, currentValue)
      case None => statement.setNull(parameterIndex, java.sql.Types.BIGINT)

  def setOptionalTimestamp(
    statement: PreparedStatement,
    parameterIndex: Int,
    timestamp: Option[Instant]
  ): Unit =
    timestamp match
      case Some(value) => statement.setTimestamp(parameterIndex, Timestamp.from(value))
      case None => statement.setNull(parameterIndex, java.sql.Types.TIMESTAMP)

  def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")

  def readOptionalLong(resultSet: ResultSet, columnName: String): Option[Long] =
    val value = resultSet.getLong(columnName)
    if resultSet.wasNull() then None else Some(value)
