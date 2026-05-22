package domains.submission.table



import cats.effect.IO
import domains.auth.model.{AuthUser, DisplayName, Username}
import domains.user.model.UserIdentity
import domains.shared.model.{PageRequest, PageResponse}
import domains.shared.sql.LikePatternSql
import domains.problem.model.{ProblemId, ProblemSlug, ProblemTitle}
import domains.submission.application.output.{SubmissionDetail, SubmissionListResponse, SubmissionSummary}
import domains.submission.model.{SubmissionId, SubmissionJudgeState, SubmissionLanguage, SubmissionSortDirection, SubmissionSourceCode, SubmissionStatus, SubmissionVerdict, SubmissionVerdictFilter}
import domains.submission.application.input.{SubmissionListRequest}
import domains.submission.table.SubmissionTableSchema.*
import domains.submission.table.SubmissionTableSql.*
import domains.submission.table.utils.SubmissionTableSupport.*

import java.nio.charset.StandardCharsets
import java.sql.{Connection, PreparedStatement, Timestamp}
import java.time.Instant
import java.util.UUID

final case class ClaimedSubmission(
  id: SubmissionId,
  problemId: ProblemId,
  problemSlug: ProblemSlug,
  language: SubmissionLanguage,
  sourceCode: SubmissionSourceCode,
  timeLimitMs: Int,
  spaceLimitMb: Int
)

object SubmissionTable:

  def initialize(connection: Connection): IO[Unit] =
    SubmissionTableSchema.initialize(connection)

  def insert(
    connection: Connection,
    problemId: ProblemId,
    problemSlug: ProblemSlug,
    problemTitle: ProblemTitle,
    submitterUsername: Username,
    language: SubmissionLanguage,
    sourceCode: SubmissionSourceCode
  ): IO[SubmissionDetail] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(insertSql)
      try
        statement.setObject(1, UUID.randomUUID())
        statement.setObject(2, problemId.value)
        statement.setString(3, submitterUsername.value)
        statement.setString(4, SubmissionLanguage.toDatabase(language))
        statement.setString(5, SubmissionStatus.toDatabase(SubmissionStatus.Queued))
        statement.setNull(6, java.sql.Types.VARCHAR)
        statement.setNull(7, java.sql.Types.LONGVARCHAR)
        statement.setNull(8, java.sql.Types.BIGINT)
        statement.setNull(9, java.sql.Types.BIGINT)
        statement.setNull(10, java.sql.Types.NUMERIC)
        statement.setNull(11, java.sql.Types.VARCHAR)
        statement.setString(12, sourceCode.value)
        statement.setTimestamp(13, Timestamp.from(now))
        statement.setNull(14, java.sql.Types.TIMESTAMP)
        statement.setNull(15, java.sql.Types.TIMESTAMP)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            SubmissionDetail(
              id = SubmissionId(resultSet.getLong("public_id")),
              problemId = problemId,
              problemSlug = problemSlug,
              problemTitle = problemTitle,
              canManage = false,
              submitter = UserIdentity(
                submitterUsername,
                DisplayName(resultSet.getString("submitter_display_name"))
              ),
              language = language,
              status = parseColumn("submissions.status", resultSet.getString("status"), SubmissionStatus.parse),
              verdict = Option(resultSet.getString("verdict")).flatMap(SubmissionVerdict.fromDatabase),
              judgeMessage = Option(resultSet.getString("judge_message")),
              timeUsedMs = readOptionalLong(resultSet, "time_used_ms"),
              memoryUsedKb = readOptionalLong(resultSet, "memory_used_kb"),
              score = readOptionalBigDecimal(resultSet, "score"),
              judgeResult = readOptionalJudgeResult(resultSet, "judge_result"),
              codeLength = sourceCode.value.getBytes(StandardCharsets.UTF_8).length,
              sourceCode = sourceCode,
              submittedAt = resultSet.getTimestamp("submitted_at").toInstant,
              startedAt = Option(resultSet.getTimestamp("started_at")).map(_.toInstant),
              finishedAt = Option(resultSet.getTimestamp("finished_at")).map(_.toInstant)
            )
          else missingInsertResult("submission")
        finally resultSet.close()
      finally statement.close()
    }

  def listVisibleTo(
    connection: Connection,
    actor: AuthUser,
    request: SubmissionListRequest,
    hasGlobalViewOverride: Boolean
  ): IO[SubmissionListResponse] =
    val normalizedPageRequest = request.pageRequest.normalized
    val normalizedRequest = request.copy(pageRequest = normalizedPageRequest)
    for
      totalItems <- IO.blocking {
        val statement = connection.prepareStatement(countSql)
        try
          bindListFilterStatement(statement, actor, normalizedRequest, includeDetailVisibility = false, hasGlobalViewOverride)
          val resultSet = statement.executeQuery()
          try if resultSet.next() then resultSet.getLong("total_items") else 0L
          finally resultSet.close()
        finally statement.close()
      }
      items <- IO.blocking {
        val statement = connection.prepareStatement(listSql(normalizedRequest.sort, normalizedRequest.direction))
        try
          val nextIndex = bindListFilterStatement(statement, actor, normalizedRequest, includeDetailVisibility = true, hasGlobalViewOverride)
          statement.setInt(nextIndex, normalizedRequest.pageRequest.pageSize)
          statement.setInt(nextIndex + 1, (normalizedRequest.pageRequest.page - 1) * normalizedRequest.pageRequest.pageSize)
          val resultSet = statement.executeQuery()
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ => readSubmissionSummary(resultSet))
              .toList
          finally resultSet.close()
        finally statement.close()
      }
    yield PageResponse(
      items = items,
      page = normalizedRequest.pageRequest.page,
      pageSize = normalizedRequest.pageRequest.pageSize,
      totalItems = totalItems
    )

  def findById(connection: Connection, submissionId: SubmissionId): IO[Option[SubmissionDetail]] =
    IO.blocking {
      val statement = connection.prepareStatement(findByIdSql)
      try
        statement.setLong(1, submissionId.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readSubmissionDetail(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }

  def claimNextForLanguages(
    connection: Connection,
    languages: List[SubmissionLanguage],
    runningState: SubmissionJudgeState
  ): IO[Option[ClaimedSubmission]] =
    if languages.isEmpty then IO.pure(None)
    else IO.blocking {
      val statement = connection.prepareStatement(claimNextForLanguagesSql(languages.size))
      try
        languages.zipWithIndex.foreach { case (language, index) =>
          statement.setString(index + 1, SubmissionLanguage.toDatabase(language))
        }
        val stateStartIndex = languages.size + 1
        statement.setString(stateStartIndex, SubmissionStatus.toDatabase(runningState.status))
        setOptionalTimestamp(statement, stateStartIndex + 1, runningState.startedAt)
        setOptionalTimestamp(statement, stateStartIndex + 2, runningState.finishedAt)
        setOptionalVerdict(statement, stateStartIndex + 3, runningState.verdict)
        setOptionalJudgeMessage(statement, stateStartIndex + 4, runningState.judgeMessage)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            Some(
              ClaimedSubmission(
                id = SubmissionId(resultSet.getLong("public_id")),
                problemId = ProblemId(resultSet.getObject("problem_id", classOf[java.util.UUID])),
                problemSlug = parseColumn("submissions.problem_slug", resultSet.getString("problem_slug"), ProblemSlug.parse),
                language = parseColumn("submissions.language", resultSet.getString("language"), SubmissionLanguage.parse),
                sourceCode = parseColumn("submissions.source_code", resultSet.getString("source_code"), SubmissionSourceCode.parse),
                timeLimitMs = resultSet.getInt("time_limit_ms"),
                spaceLimitMb = resultSet.getInt("space_limit_mb")
              )
            )
          else None
        finally resultSet.close()
      finally statement.close()
    }

  def updateJudgeState(
    connection: Connection,
    submissionId: SubmissionId,
    judgeState: SubmissionJudgeState
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(updateJudgeStateSql)
      try
        statement.setString(1, SubmissionStatus.toDatabase(judgeState.status))
        setOptionalVerdict(statement, 2, judgeState.verdict)
        setOptionalJudgeMessage(statement, 3, judgeState.judgeMessage)
        setOptionalLong(statement, 4, judgeState.timeUsedMs)
        setOptionalLong(statement, 5, judgeState.memoryUsedKb)
        setOptionalBigDecimal(statement, 6, judgeState.score)
        setOptionalJudgeResult(statement, 7, judgeState.judgeResult)
        setOptionalTimestamp(statement, 8, judgeState.startedAt)
        setOptionalTimestamp(statement, 9, judgeState.finishedAt)
        statement.setLong(10, submissionId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  def deleteById(connection: Connection, submissionId: SubmissionId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteByIdSql)
      try
        statement.setLong(1, submissionId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private def bindListFilterStatement(
    statement: PreparedStatement,
    actor: AuthUser,
    request: SubmissionListRequest,
    includeDetailVisibility: Boolean,
    hasGlobalViewOverride: Boolean
  ): Int =
    val afterDetailVisibility =
      if includeDetailVisibility then bindVisibility(statement, 1, actor, hasGlobalViewOverride)
      else 1

    val afterSummaryVisibility = bindVisibility(statement, afterDetailVisibility, actor, hasGlobalViewOverride)
    val afterUserQuery = bindUserQuery(statement, afterSummaryVisibility, request.userQuery)
    val afterProblemQuery = bindProblemQuery(statement, afterUserQuery, request.problemQuery)

    val isAllVerdict = request.verdict == SubmissionVerdictFilter.All
    val isPendingVerdict = request.verdict == SubmissionVerdictFilter.Pending
    val specificVerdict = request.verdict match
      case SubmissionVerdictFilter.Accepted => Some(SubmissionVerdict.Accepted)
      case SubmissionVerdictFilter.WrongAnswer => Some(SubmissionVerdict.WrongAnswer)
      case SubmissionVerdictFilter.CompileError => Some(SubmissionVerdict.CompileError)
      case SubmissionVerdictFilter.RuntimeError => Some(SubmissionVerdict.RuntimeError)
      case SubmissionVerdictFilter.TimeLimitExceeded => Some(SubmissionVerdict.TimeLimitExceeded)
      case SubmissionVerdictFilter.SystemError => Some(SubmissionVerdict.SystemError)
      case SubmissionVerdictFilter.All | SubmissionVerdictFilter.Pending => None

    val afterAllVerdict = bindBoolean(statement, afterProblemQuery, isAllVerdict)
    val afterPendingVerdict = bindBoolean(statement, afterAllVerdict, isPendingVerdict)
    val afterSpecificVerdict = bindBoolean(statement, afterPendingVerdict, specificVerdict.nonEmpty)
    bindNullableString(statement, afterSpecificVerdict, specificVerdict.map(SubmissionVerdict.toDatabase), java.sql.Types.VARCHAR)

  private def bindVisibility(
    statement: PreparedStatement,
    startIndex: Int,
    actor: AuthUser,
    hasGlobalViewOverride: Boolean
  ): Int =
    val afterGlobalOverride = bindBoolean(statement, startIndex, hasGlobalViewOverride)
    val afterOwnUsername = bindString(statement, afterGlobalOverride, actor.username.value)
    val afterProblemViewerGrant = bindString(statement, afterOwnUsername, actor.username.value)
    val afterProblemGroupGrant = bindString(statement, afterProblemViewerGrant, actor.username.value)
    val afterProblemSetOverride = bindBoolean(statement, afterProblemGroupGrant, hasGlobalViewOverride)
    val afterProblemSetViewerGrant = bindString(statement, afterProblemSetOverride, actor.username.value)
    bindString(statement, afterProblemSetViewerGrant, actor.username.value)

  private def bindUserQuery(
    statement: PreparedStatement,
    startIndex: Int,
    rawQuery: Option[domains.submission.model.SubmissionUserQuery]
  ): Int =
    val searchPattern = rawQuery.map(query => LikePatternSql.fromRaw(query.value))
    val afterEnabledFlag = bindBoolean(statement, startIndex, rawQuery.nonEmpty)
    val afterUsernamePattern = bindString(statement, afterEnabledFlag, searchPattern.map(_.containsPattern).getOrElse(""))
    bindString(statement, afterUsernamePattern, searchPattern.map(_.containsPattern).getOrElse(""))

  private def bindProblemQuery(
    statement: PreparedStatement,
    startIndex: Int,
    rawQuery: Option[domains.submission.model.SubmissionProblemQuery]
  ): Int =
    val searchPattern = rawQuery.map(query => LikePatternSql.fromRaw(query.value))
    val afterEnabledFlag = bindBoolean(statement, startIndex, rawQuery.nonEmpty)
    val afterSlugPattern = bindString(statement, afterEnabledFlag, searchPattern.map(_.containsPattern).getOrElse(""))
    bindString(statement, afterSlugPattern, searchPattern.map(_.containsPattern).getOrElse(""))

  private def bindBoolean(statement: PreparedStatement, index: Int, value: Boolean): Int =
    statement.setBoolean(index, value)
    index + 1

  private def bindString(statement: PreparedStatement, index: Int, value: String): Int =
    statement.setString(index, value)
    index + 1

  private def bindNullableString(statement: PreparedStatement, index: Int, value: Option[String], sqlType: Int): Int =
    value match
      case Some(currentValue) => statement.setString(index, currentValue)
      case None => statement.setNull(index, sqlType)
    index + 1
