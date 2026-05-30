package domains.submission.table.submission



import domains.problem.objects.{ProblemId, ProblemSlug, ProblemTitle}
import domains.submission.objects.response.{SubmissionDetail, SubmissionSummary}
import domains.submission.objects.{SubmissionId, SubmissionLanguage, SubmissionSourceCode, SubmissionStatus, SubmissionVerdict}
import database.utils.UserIdentitySql
import domains.user.objects.{DisplayName, UserIdentity, Username}
import io.circe.parser.decode
import io.circe.syntax.*
import judgeprotocol.objects.response.JudgeResult

import java.sql.{PreparedStatement, ResultSet, Timestamp}
import java.time.Instant

object SubmissionTableSupport:

  def readUserIdentity(resultSet: ResultSet, prefix: String): UserIdentity =
    val row = UserIdentitySql.readUserIdentityRow(resultSet, prefix)
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )

  def readSubmissionSummary(resultSet: ResultSet): SubmissionSummary =
    SubmissionSummary(
      id = SubmissionId(resultSet.getLong("public_id")),
      problemId = ProblemId(resultSet.getObject("problem_id", classOf[java.util.UUID])),
      problemSlug = parseColumn("submissions.problem_slug", resultSet.getString("problem_slug"), ProblemSlug.parse),
      problemTitle = parseColumn("submissions.problem_title", resultSet.getString("problem_title"), ProblemTitle.parse),
      canViewDetail = resultSet.getBoolean("can_view_detail"),
      submitter = readUserIdentity(resultSet, "submitter"),
      language = parseColumn("submissions.language", resultSet.getString("language"), SubmissionLanguage.parse),
      status = parseColumn("submissions.status", resultSet.getString("status"), SubmissionStatus.parse),
      verdict = Option(resultSet.getString("verdict")).flatMap(decodeSubmissionVerdictColumn),
      timeUsedMs = readOptionalLong(resultSet, "time_used_ms"),
      memoryUsedKb = readOptionalLong(resultSet, "memory_used_kb"),
      score = readOptionalBigDecimal(resultSet, "score"),
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
      verdict = Option(resultSet.getString("verdict")).flatMap(decodeSubmissionVerdictColumn),
      judgeMessage = Option(resultSet.getString("judge_message")),
      timeUsedMs = readOptionalLong(resultSet, "time_used_ms"),
      memoryUsedKb = readOptionalLong(resultSet, "memory_used_kb"),
      score = readOptionalBigDecimal(resultSet, "score"),
      judgeResult = readOptionalJudgeResult(resultSet, "judge_result"),
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
      case Some(value) => statement.setString(parameterIndex, encodeSubmissionVerdictColumn(value))
      case None => statement.setNull(parameterIndex, java.sql.Types.VARCHAR)

  def encodeSubmissionLanguageColumn(value: SubmissionLanguage): String =
    value match
      case SubmissionLanguage.Cpp17 => "cpp17"
      case SubmissionLanguage.Python3 => "python3"

  def encodeSubmissionStatusColumn(value: SubmissionStatus): String =
    value match
      case SubmissionStatus.Queued => "queued"
      case SubmissionStatus.Running => "running"
      case SubmissionStatus.Completed => "completed"
      case SubmissionStatus.Failed => "failed"

  def encodeSubmissionVerdictColumn(value: SubmissionVerdict): String =
    value match
      case SubmissionVerdict.Accepted => "accepted"
      case SubmissionVerdict.WrongAnswer => "wrong_answer"
      case SubmissionVerdict.CompileError => "compile_error"
      case SubmissionVerdict.RuntimeError => "runtime_error"
      case SubmissionVerdict.TimeLimitExceeded => "time_limit_exceeded"
      case SubmissionVerdict.SystemError => "system_error"

  def decodeSubmissionVerdictColumn(value: String): Option[SubmissionVerdict] =
    SubmissionVerdict.parse(value).toOption

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

  def setOptionalBigDecimal(
    statement: PreparedStatement,
    parameterIndex: Int,
    value: Option[BigDecimal]
  ): Unit =
    value match
      case Some(currentValue) => statement.setBigDecimal(parameterIndex, currentValue.bigDecimal)
      case None => statement.setNull(parameterIndex, java.sql.Types.NUMERIC)

  def setOptionalJudgeResult(
    statement: PreparedStatement,
    parameterIndex: Int,
    value: Option[JudgeResult]
  ): Unit =
    value match
      case Some(currentValue) => statement.setString(parameterIndex, currentValue.asJson.noSpaces)
      case None => statement.setNull(parameterIndex, java.sql.Types.VARCHAR)

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

  def readOptionalBigDecimal(resultSet: ResultSet, columnName: String): Option[BigDecimal] =
    Option(resultSet.getBigDecimal(columnName)).map(BigDecimal(_))

  def readOptionalJudgeResult(resultSet: ResultSet, columnName: String): Option[JudgeResult] =
    Option(resultSet.getString(columnName)).map { raw =>
      decode[JudgeResult](raw).fold(error => throw IllegalStateException(s"Invalid judge result JSON: ${error.getMessage}"), identity)
    }
