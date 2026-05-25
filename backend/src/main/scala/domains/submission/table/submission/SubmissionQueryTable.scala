package domains.submission.table.submission

import cats.effect.IO
import database.utils.{LikePatternSql, UserIdentitySql}
import domains.auth.model.AuthUser
import domains.submission.model.{SubmissionId, SubmissionVerdict}
import domains.submission.model.request.{SubmissionListRequest, SubmissionProblemQuery, SubmissionSort, SubmissionSortDirection, SubmissionUserQuery, SubmissionVerdictFilter}
import domains.submission.model.response.{SubmissionDetail, SubmissionListResponse}
import domains.submission.table.submission.SubmissionTableSupport.*
import shared.model.PageResponse

import java.sql.{Connection, PreparedStatement}

object SubmissionQueryTable:

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
