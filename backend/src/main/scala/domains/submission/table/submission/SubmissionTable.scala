package domains.submission.table.submission



import cats.effect.IO
import domains.auth.model.AuthUser
import domains.user.model.{DisplayName, Username}
import domains.user.model.UserIdentity
import shared.model.{PageRequest, PageResponse}
import shared.sql.LikePatternSql
import domains.problem.model.{ProblemId, ProblemSlug, ProblemTitle}
import domains.submission.application.input.{SubmissionListRequest, SubmissionProblemQuery, SubmissionUserQuery, SubmissionVerdictFilter}
import domains.submission.application.output.{ClaimedSubmission, SubmissionDetail, SubmissionListResponse, SubmissionSummary}
import domains.submission.model.{SubmissionId, SubmissionJudgeState, SubmissionLanguage, SubmissionSourceCode, SubmissionStatus, SubmissionVerdict}
import domains.submission.table.submission.SubmissionTableSchema.*
import domains.submission.table.submission.SubmissionTableSupport.*

import java.nio.charset.StandardCharsets
import java.sql.{Connection, PreparedStatement, Timestamp}
import java.time.Instant
import java.util.UUID
import domains.submission.application.input.{SubmissionSort, SubmissionSortDirection}
import shared.sql.UserIdentitySql

object SubmissionTable:

  def initialize(connection: Connection): IO[Unit] =
    SubmissionTableSchema.initialize(connection)

  private val insertSQL: String =
    s"""
      |insert into submissions (id, public_id, problem_id, submitter_username, language, status, verdict, judge_message, time_used_ms, memory_used_kb, score, judge_result, source_code, submitted_at, started_at, finished_at)
      |values (?, nextval('submission_public_id_seq'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
      |returning public_id, status, verdict, judge_message, time_used_ms, memory_used_kb, score, judge_result::text as judge_result, ${UserIdentitySql.returningColumns("submitter_username", "submitter")}, submitted_at, started_at, finished_at
      |""".stripMargin

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
      val statement = connection.prepareStatement(insertSQL)
      try
        statement.setObject(1, UUID.randomUUID())
        statement.setObject(2, problemId.value)
        statement.setString(3, submitterUsername.value)
        statement.setString(4, encodeSubmissionLanguageColumn(language))
        statement.setString(5, encodeSubmissionStatusColumn(SubmissionStatus.Queued))
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
              verdict = Option(resultSet.getString("verdict")).flatMap(decodeSubmissionVerdictColumn),
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

  private def toSqlDirection(direction: SubmissionSortDirection): String =
    direction match
      case SubmissionSortDirection.Asc => "asc"
      case SubmissionSortDirection.Desc => "desc"

  private def orderByClause(sort: SubmissionSort, direction: SubmissionSortDirection): String =
    val submittedDirection = toSqlDirection(direction)
    sort match
      case SubmissionSort.Submitted =>
        s"s.submitted_at $submittedDirection, s.public_id $submittedDirection"
      case SubmissionSort.Time =>
        s"s.time_used_ms $submittedDirection nulls last, s.submitted_at desc, s.public_id desc"
      case SubmissionSort.Memory =>
        s"s.memory_used_kb $submittedDirection nulls last, s.submitted_at desc, s.public_id desc"
      case SubmissionSort.CodeLength =>
        s"octet_length(s.source_code) $submittedDirection, s.submitted_at desc, s.public_id desc"

  private val summaryVisibilityPredicate: String =
    """
      |(
      |  ? = true
      |  or s.submitter_username = ?
      |  or (
      |    p.others_submission_access in ('summary', 'detail')
      |    and (
      |      p.base_access = 'public'
      |      or exists (
      |        select 1
      |        from resource_access_grants rag
      |        where rag.resource_kind = 'problem'
      |          and rag.resource_id = p.id
      |          and rag.grant_role = 'viewer'
      |          and rag.subject_kind = 'user'
      |          and rag.subject_key = ?
      |      )
      |      or exists (
      |        select 1
      |        from resource_access_grants rag
      |        join user_groups ug on ug.slug = rag.subject_key
      |        join user_group_memberships ugm on ugm.user_group_id = ug.id
      |        where rag.resource_kind = 'problem'
      |          and rag.resource_id = p.id
      |          and rag.grant_role = 'viewer'
      |          and rag.subject_kind = 'user_group'
      |          and ugm.username = ?
      |      )
      |      or exists (
      |        select 1
      |        from problem_set_problems psp
      |        join problem_sets ps on ps.id = psp.problem_set_id
      |        where psp.problem_id = p.id
      |          and (
      |            ? = true
      |            or ps.base_access = 'public'
      |            or exists (
      |              select 1
      |              from resource_access_grants rag
      |              where rag.resource_kind = 'problem_set'
      |                and rag.resource_id = ps.id
      |                and rag.grant_role = 'viewer'
      |                and rag.subject_kind = 'user'
      |                and rag.subject_key = ?
      |            )
      |            or exists (
      |              select 1
      |              from resource_access_grants rag
      |              join user_groups ug on ug.slug = rag.subject_key
      |              join user_group_memberships ugm on ugm.user_group_id = ug.id
      |              where rag.resource_kind = 'problem_set'
      |                and rag.resource_id = ps.id
      |                and rag.grant_role = 'viewer'
      |                and rag.subject_kind = 'user_group'
      |                and ugm.username = ?
      |            )
      |          )
      |      )
      |    )
      |  )
      |)
      |""".stripMargin

  private val usernameFilterPredicate: String =
    """
      |(? = false or lower(s.submitter_username) like lower(?) escape '\' or lower(au.display_name) like lower(?) escape '\')
      |""".stripMargin

  private val problemQueryFilterPredicate: String =
    """
      |(? = false or lower(p.slug) like lower(?) escape '\' or lower(p.title) like lower(?) escape '\')
      |""".stripMargin

  private val verdictFilterPredicate: String =
    """
      |(
      |  ? = true
      |  or (? = true and s.verdict is null)
      |  or (? = true and s.verdict = ?)
      |)
      |""".stripMargin

  private val detailVisibilityPredicate: String =
    """
      |(
      |  ? = true
      |  or s.submitter_username = ?
      |  or (
      |    p.others_submission_access = 'detail'
      |    and (
      |      p.base_access = 'public'
      |      or exists (
      |        select 1
      |        from resource_access_grants rag
      |        where rag.resource_kind = 'problem'
      |          and rag.resource_id = p.id
      |          and rag.grant_role = 'viewer'
      |          and rag.subject_kind = 'user'
      |          and rag.subject_key = ?
      |      )
      |      or exists (
      |        select 1
      |        from resource_access_grants rag
      |        join user_groups ug on ug.slug = rag.subject_key
      |        join user_group_memberships ugm on ugm.user_group_id = ug.id
      |        where rag.resource_kind = 'problem'
      |          and rag.resource_id = p.id
      |          and rag.grant_role = 'viewer'
      |          and rag.subject_kind = 'user_group'
      |          and ugm.username = ?
      |      )
      |      or exists (
      |        select 1
      |        from problem_set_problems psp
      |        join problem_sets ps on ps.id = psp.problem_set_id
      |        where psp.problem_id = p.id
      |          and (
      |            ? = true
      |            or ps.base_access = 'public'
      |            or exists (
      |              select 1
      |              from resource_access_grants rag
      |              where rag.resource_kind = 'problem_set'
      |                and rag.resource_id = ps.id
      |                and rag.grant_role = 'viewer'
      |                and rag.subject_kind = 'user'
      |                and rag.subject_key = ?
      |            )
      |            or exists (
      |              select 1
      |              from resource_access_grants rag
      |              join user_groups ug on ug.slug = rag.subject_key
      |              join user_group_memberships ugm on ugm.user_group_id = ug.id
      |              where rag.resource_kind = 'problem_set'
      |                and rag.resource_id = ps.id
      |                and rag.grant_role = 'viewer'
      |                and rag.subject_kind = 'user_group'
      |                and ugm.username = ?
      |            )
      |          )
      |      )
      |    )
      |  )
      |)
      |""".stripMargin

  private val countSQL: String =
    s"""
      |select count(*) as total_items
      |from submissions s
      |join problems p on p.id = s.problem_id
      |${UserIdentitySql.joinAuthUsers("s.submitter_username", "au")}
      |where
      |  $summaryVisibilityPredicate
      |  and $usernameFilterPredicate
      |  and $problemQueryFilterPredicate
      |  and $verdictFilterPredicate
      |""".stripMargin

  private def listSQL(sort: SubmissionSort, direction: SubmissionSortDirection): String =
    s"""
      |select s.public_id,
      |       s.problem_id,
      |       p.slug as problem_slug,
      |       p.title as problem_title,
      |       $detailVisibilityPredicate as can_view_detail,
      |       ${UserIdentitySql.selectColumns("s.submitter_username", "submitter", "au")},
      |       s.language,
      |       s.status,
      |       s.verdict,
      |       s.time_used_ms,
      |       s.memory_used_kb,
      |       s.score,
      |       octet_length(s.source_code) as code_length,
      |       s.submitted_at,
      |       s.started_at,
      |       s.finished_at
      |from submissions s
      |join problems p on p.id = s.problem_id
      |${UserIdentitySql.joinAuthUsers("s.submitter_username", "au")}
      |where
      |  $summaryVisibilityPredicate
      |  and $usernameFilterPredicate
      |  and $problemQueryFilterPredicate
      |  and $verdictFilterPredicate
      |order by ${orderByClause(sort, direction)}
      |limit ? offset ?
      |""".stripMargin

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
        val statement = connection.prepareStatement(countSQL)
        try
          bindListFilterStatement(statement, actor, normalizedRequest, includeDetailVisibility = false, hasGlobalViewOverride)
          val resultSet = statement.executeQuery()
          try if resultSet.next() then resultSet.getLong("total_items") else 0L
          finally resultSet.close()
        finally statement.close()
      }
      items <- IO.blocking {
        val statement = connection.prepareStatement(listSQL(normalizedRequest.sort, normalizedRequest.direction))
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

  private val findByIdSQL: String =
    s"""
      |select s.public_id, s.problem_id, p.slug as problem_slug, p.title as problem_title, ${UserIdentitySql.selectColumns("s.submitter_username", "submitter", "au")}, s.language, s.status, s.verdict, s.judge_message, s.time_used_ms, s.memory_used_kb, s.score, s.judge_result::text as judge_result, octet_length(s.source_code) as code_length, s.source_code, s.submitted_at, s.started_at, s.finished_at
      |from submissions s
      |join problems p on p.id = s.problem_id
      |${UserIdentitySql.joinAuthUsers("s.submitter_username", "au")}
      |where s.public_id = ?
      |""".stripMargin

  def findById(connection: Connection, submissionId: SubmissionId): IO[Option[SubmissionDetail]] =
    IO.blocking {
      val statement = connection.prepareStatement(findByIdSQL)
      try
        statement.setLong(1, submissionId.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readSubmissionDetail(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }

  private def claimNextForLanguagesSQL(languageCount: Int): String =
    val placeholders = List.fill(languageCount)("?").mkString(", ")
    s"""
      |with next_submission as (
      |  select s.id
      |  from submissions s
      |  join problems p on p.id = s.problem_id
      |  where s.status = 'queued'
      |    and s.language in ($placeholders)
      |    and p.ready = true
      |  order by s.submitted_at asc, s.public_id asc
      |  for update skip locked
      |  limit 1
      |)
      |update submissions s
      |set status = ?,
      |    started_at = ?,
      |    finished_at = ?,
      |    verdict = ?,
      |    judge_message = ?,
      |    time_used_ms = null,
      |    memory_used_kb = null,
      |    score = null,
      |    judge_result = null
      |from next_submission ns, problems p
      |where s.id = ns.id
      |  and p.id = s.problem_id
      |returning s.public_id, s.problem_id, p.slug as problem_slug, s.language, s.source_code, p.time_limit_ms, p.space_limit_mb
      |""".stripMargin

  def claimNextForLanguages(
    connection: Connection,
    languages: List[SubmissionLanguage],
    runningState: SubmissionJudgeState
  ): IO[Option[ClaimedSubmission]] =
    if languages.isEmpty then IO.pure(None)
    else IO.blocking {
      val statement = connection.prepareStatement(claimNextForLanguagesSQL(languages.size))
      try
        languages.zipWithIndex.foreach { case (language, index) =>
          statement.setString(index + 1, encodeSubmissionLanguageColumn(language))
        }
        val stateStartIndex = languages.size + 1
        statement.setString(stateStartIndex, encodeSubmissionStatusColumn(runningState.status))
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

  private val updateJudgeStateSQL: String =
    """
      |update submissions
      |set status = ?, verdict = ?, judge_message = ?, time_used_ms = ?, memory_used_kb = ?, score = ?, judge_result = ?::jsonb, started_at = ?, finished_at = ?
      |where public_id = ?
      |""".stripMargin

  def updateJudgeState(
    connection: Connection,
    submissionId: SubmissionId,
    judgeState: SubmissionJudgeState
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(updateJudgeStateSQL)
      try
        statement.setString(1, encodeSubmissionStatusColumn(judgeState.status))
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

  private val deleteByIdSQL: String =
    """
      |delete from submissions
      |where public_id = ?
      |""".stripMargin

  def deleteById(connection: Connection, submissionId: SubmissionId): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteByIdSQL)
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
    bindNullableString(statement, afterSpecificVerdict, specificVerdict.map(encodeSubmissionVerdictColumn), java.sql.Types.VARCHAR)

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
    rawQuery: Option[SubmissionUserQuery]
  ): Int =
    val searchPattern = rawQuery.map(query => LikePatternSql.fromRaw(query.value))
    val afterEnabledFlag = bindBoolean(statement, startIndex, rawQuery.nonEmpty)
    val afterUsernamePattern = bindString(statement, afterEnabledFlag, searchPattern.map(_.containsPattern).getOrElse(""))
    bindString(statement, afterUsernamePattern, searchPattern.map(_.containsPattern).getOrElse(""))

  private def bindProblemQuery(
    statement: PreparedStatement,
    startIndex: Int,
    rawQuery: Option[SubmissionProblemQuery]
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
