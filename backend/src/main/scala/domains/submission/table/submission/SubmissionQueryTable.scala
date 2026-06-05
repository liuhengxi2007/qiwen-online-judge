package domains.submission.table.submission

import cats.effect.IO
import database.utils.{LikePatternSql, UserIdentitySql}
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestId
import domains.problem.objects.ProblemId
import domains.submission.objects.{SubmissionId, SubmissionVerdict}
import domains.submission.objects.internal.{SubmissionDetailRecord, SubmissionProgramManifest}
import domains.submission.objects.request.{SubmissionListRequest, SubmissionProblemQuery, SubmissionSort, SubmissionSortDirection, SubmissionUserQuery, SubmissionVerdictFilter}
import domains.submission.objects.response.SubmissionListResponse
import domains.submission.table.submission.SubmissionTableSupport.*
import shared.objects.PageResponse

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
        s"s.code_length $submittedDirection, s.submitted_at desc, s.public_id desc"

  private val ordinarySummaryVisibilityPredicate: String =
    """
      |(
      |  ? = true
      |  or s.submitter_username = ?
      |  or (
      |    p.other_user_submission_access in ('summary', 'detail')
      |    and (
      |      p.base_access = 'public'
      |      or exists (
      |        select 1
      |        from problem_access_grants pag
      |        where pag.problem_id = p.id
      |          and pag.grant_role = 'viewer'
      |          and pag.subject_kind = 'user'
      |          and pag.subject_key = ?
      |      )
      |      or exists (
      |        select 1
      |        from problem_access_grants pag
      |        join user_groups ug on ug.slug = pag.subject_key
      |        join user_group_memberships ugm on ugm.user_group_id = ug.id
      |        where pag.problem_id = p.id
      |          and pag.grant_role = 'viewer'
      |          and pag.subject_kind = 'user_group'
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
      |              from problem_set_access_grants psag
      |              where psag.problem_set_id = ps.id
      |                and psag.grant_role = 'viewer'
      |                and psag.subject_kind = 'user'
      |                and psag.subject_key = ?
      |            )
      |            or exists (
      |              select 1
      |              from problem_set_access_grants psag
      |              join user_groups ug on ug.slug = psag.subject_key
      |              join user_group_memberships ugm on ugm.user_group_id = ug.id
      |              where psag.problem_set_id = ps.id
      |                and psag.grant_role = 'viewer'
      |                and psag.subject_kind = 'user_group'
      |                and ugm.username = ?
      |            )
      |          )
      |      )
      |    )
      |  )
      |)
      |""".stripMargin

  private val problemManagerVisibilityPredicate: String =
    """
      |(
      |  ? = true
      |  or exists (
      |    select 1
      |    from problem_access_grants pag
      |    where pag.problem_id = p.id
      |      and pag.grant_role = 'manager'
      |      and pag.subject_kind = 'user'
      |      and pag.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from problem_access_grants pag
      |    join user_groups ug on ug.slug = pag.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where pag.problem_id = p.id
      |      and pag.grant_role = 'manager'
      |      and pag.subject_kind = 'user_group'
      |      and ugm.username = ?
      |  )
      |)
      |""".stripMargin

  private val visibleContestPredicate: String =
    """
      |(
      |  ? = true
      |  or c.base_access = 'public'
      |  or exists (
      |    select 1
      |    from contest_access_grants cag
      |    where cag.contest_id = c.id
      |      and cag.grant_role in ('viewer', 'manager')
      |      and cag.subject_kind = 'user'
      |      and cag.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from contest_access_grants cag
      |    join user_groups ug on ug.slug = cag.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where cag.contest_id = c.id
      |      and cag.grant_role in ('viewer', 'manager')
      |      and cag.subject_kind = 'user_group'
      |      and ugm.username = ?
      |  )
      |  or exists (
      |    select 1
      |    from contest_registrations cr
      |    where cr.contest_id = c.id
      |      and cr.username = ?
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

  private val globalSubmissionSourcePredicate: String =
    s"""
      |(
      |  s.contest_id is null
      |  or exists (
      |    select 1
      |    from contests c
      |    where c.id = s.contest_id
      |      and c.end_at < now()
      |      and $visibleContestPredicate
      |  )
      |)
      |""".stripMargin

  private val visibleEndedSourceContestPredicate: String =
    s"""
      |exists (
      |  select 1
      |  from contests c
      |  where c.id = s.contest_id
      |    and c.end_at < now()
      |    and $visibleContestPredicate
      |)
      |""".stripMargin

  private val ordinaryDetailVisibilityPredicate: String =
    """
      |(
      |  ? = true
      |  or s.submitter_username = ?
      |  or (
      |    p.other_user_submission_access = 'detail'
      |    and (
      |      p.base_access = 'public'
      |      or exists (
      |        select 1
      |        from problem_access_grants pag
      |        where pag.problem_id = p.id
      |          and pag.grant_role = 'viewer'
      |          and pag.subject_kind = 'user'
      |          and pag.subject_key = ?
      |      )
      |      or exists (
      |        select 1
      |        from problem_access_grants pag
      |        join user_groups ug on ug.slug = pag.subject_key
      |        join user_group_memberships ugm on ugm.user_group_id = ug.id
      |        where pag.problem_id = p.id
      |          and pag.grant_role = 'viewer'
      |          and pag.subject_kind = 'user_group'
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
      |              from problem_set_access_grants psag
      |              where psag.problem_set_id = ps.id
      |                and psag.grant_role = 'viewer'
      |                and psag.subject_kind = 'user'
      |                and psag.subject_key = ?
      |            )
      |            or exists (
      |              select 1
      |              from problem_set_access_grants psag
      |              join user_groups ug on ug.slug = psag.subject_key
      |              join user_group_memberships ugm on ugm.user_group_id = ug.id
      |              where psag.problem_set_id = ps.id
      |                and psag.grant_role = 'viewer'
      |                and psag.subject_kind = 'user_group'
      |                and ugm.username = ?
      |            )
      |          )
      |      )
      |    )
      |  )
      |)
      |""".stripMargin

  private val detailVisibilityPredicate: String =
    s"""
      |(
      |  $problemManagerVisibilityPredicate
      |  or (
      |    s.contest_id is null
      |    and $ordinaryDetailVisibilityPredicate
      |  )
      |  or (
      |    s.contest_id is not null
      |    and $visibleEndedSourceContestPredicate
      |  )
      |)
      |""".stripMargin

  private val summaryVisibilityPredicate: String =
    s"""
      |(
      |  $problemManagerVisibilityPredicate
      |  or (
      |    s.contest_id is null
      |    and $ordinarySummaryVisibilityPredicate
      |  )
      |  or (
      |    s.contest_id is not null
      |    and $visibleEndedSourceContestPredicate
      |  )
      |)
      |""".stripMargin

  private val countSQL: String =
    s"""
      |select count(*) as total_items
      |from submissions s
      |join problems p on p.id = s.problem_id
      |left join contests source_c on source_c.id = s.contest_id
      |${UserIdentitySql.joinUserProfiles("s.submitter_username", "au")}
      |where
      |  $globalSubmissionSourcePredicate
      |  and
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
      |       source_c.slug as source_contest_slug,
      |       source_c.title as source_contest_title,
      |       ${UserIdentitySql.selectColumns("s.submitter_username", "submitter", "au")},
      |       s.language,
      |       s.status,
      |       s.verdict,
      |       s.time_used_ms,
      |       s.memory_used_kb,
      |       s.score,
      |       s.code_length,
      |       s.submitted_at,
      |       s.started_at,
      |       s.finished_at
      |from submissions s
      |join problems p on p.id = s.problem_id
      |left join contests source_c on source_c.id = s.contest_id
      |${UserIdentitySql.joinUserProfiles("s.submitter_username", "au")}
      |where
      |  $globalSubmissionSourcePredicate
      |  and
      |  $summaryVisibilityPredicate
      |  and $usernameFilterPredicate
      |  and $problemQueryFilterPredicate
      |  and $verdictFilterPredicate
      |order by ${orderByClause(sort, direction)}
      |limit ? offset ?
      |""".stripMargin

  def listVisibleTo(
    connection: Connection,
    actor: AuthenticatedUser,
    request: SubmissionListRequest,
    hasGlobalViewOverride: Boolean
  ): IO[SubmissionListResponse] =
    val normalizedPageRequest = request.pageRequest.normalized
    val normalizedRequest = request.copy(pageRequest = normalizedPageRequest)
    for
      totalItems <- IO.blocking {
        val statement = connection.prepareStatement(countSQL)
        try
          bindGlobalListFilterStatement(statement, actor, normalizedRequest, includeDetailVisibility = false, hasGlobalViewOverride)
          val resultSet = statement.executeQuery()
          try if resultSet.next() then resultSet.getLong("total_items") else 0L
          finally resultSet.close()
        finally statement.close()
      }
      items <- IO.blocking {
        val statement = connection.prepareStatement(listSQL(normalizedRequest.sort, normalizedRequest.direction))
        try
          val nextIndex = bindGlobalListFilterStatement(statement, actor, normalizedRequest, includeDetailVisibility = true, hasGlobalViewOverride)
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

  private val contestSubmissionOwnerPredicate: String =
    """
      |(? = true or s.submitter_username = ?)
      |""".stripMargin

  private val countContestSQL: String =
    s"""
      |select count(*) as total_items
      |from submissions s
      |join problems p on p.id = s.problem_id
      |left join contests source_c on source_c.id = s.contest_id
      |${UserIdentitySql.joinUserProfiles("s.submitter_username", "au")}
      |where
      |  s.contest_id = ?
      |  and $contestSubmissionOwnerPredicate
      |  and $usernameFilterPredicate
      |  and $problemQueryFilterPredicate
      |  and $verdictFilterPredicate
      |""".stripMargin

  private def listContestSQL(sort: SubmissionSort, direction: SubmissionSortDirection): String =
    s"""
      |select s.public_id,
      |       s.problem_id,
      |       p.slug as problem_slug,
      |       p.title as problem_title,
      |       $contestSubmissionOwnerPredicate as can_view_detail,
      |       source_c.slug as source_contest_slug,
      |       source_c.title as source_contest_title,
      |       ${UserIdentitySql.selectColumns("s.submitter_username", "submitter", "au")},
      |       s.language,
      |       s.status,
      |       s.verdict,
      |       s.time_used_ms,
      |       s.memory_used_kb,
      |       s.score,
      |       s.code_length,
      |       s.submitted_at,
      |       s.started_at,
      |       s.finished_at
      |from submissions s
      |join problems p on p.id = s.problem_id
      |left join contests source_c on source_c.id = s.contest_id
      |${UserIdentitySql.joinUserProfiles("s.submitter_username", "au")}
      |where
      |  s.contest_id = ?
      |  and $contestSubmissionOwnerPredicate
      |  and $usernameFilterPredicate
      |  and $problemQueryFilterPredicate
      |  and $verdictFilterPredicate
      |order by ${orderByClause(sort, direction)}
      |limit ? offset ?
      |""".stripMargin

  def listVisibleForContest(
    connection: Connection,
    actor: AuthenticatedUser,
    contestId: ContestId,
    request: SubmissionListRequest,
    canViewAllContestSubmissions: Boolean
  ): IO[SubmissionListResponse] =
    val normalizedPageRequest = request.pageRequest.normalized
    val normalizedRequest = request.copy(pageRequest = normalizedPageRequest)
    for
      totalItems <- IO.blocking {
        val statement = connection.prepareStatement(countContestSQL)
        try
          bindContestListFilterStatement(
            statement,
            actor,
            contestId,
            normalizedRequest,
            includeDetailVisibility = false,
            canViewAllContestSubmissions
          )
          val resultSet = statement.executeQuery()
          try if resultSet.next() then resultSet.getLong("total_items") else 0L
          finally resultSet.close()
        finally statement.close()
      }
      items <- IO.blocking {
        val statement = connection.prepareStatement(listContestSQL(normalizedRequest.sort, normalizedRequest.direction))
        try
          val nextIndex = bindContestListFilterStatement(
            statement,
            actor,
            contestId,
            normalizedRequest,
            includeDetailVisibility = true,
            canViewAllContestSubmissions
          )
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
      |select s.public_id, s.problem_id, p.slug as problem_slug, p.title as problem_title, source_c.slug as source_contest_slug, source_c.title as source_contest_title, ${UserIdentitySql.selectColumns("s.submitter_username", "submitter", "au")}, s.language, s.status, s.verdict, s.time_used_ms, s.memory_used_kb, s.score, s.judge_result::text as judge_result, s.code_length, s.program_manifest::text as program_manifest, s.submitted_at, s.started_at, s.finished_at
      |from submissions s
      |join problems p on p.id = s.problem_id
      |left join contests source_c on source_c.id = s.contest_id
      |${UserIdentitySql.joinUserProfiles("s.submitter_username", "au")}
      |where s.public_id = ?
      |""".stripMargin

  def findById(connection: Connection, submissionId: SubmissionId): IO[Option[SubmissionDetailRecord]] =
    IO.blocking {
      val statement = connection.prepareStatement(findByIdSQL)
      try
        statement.setLong(1, submissionId.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readSubmissionDetailRecord(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }

  private val listProgramManifestsForProblemSQL: String =
    """
      |select program_manifest::text as program_manifest
      |from submissions
      |where problem_id = ?
      |""".stripMargin

  def listProgramManifestsForProblem(connection: Connection, problemId: ProblemId): IO[List[SubmissionProgramManifest]] =
    IO.blocking {
      val statement = connection.prepareStatement(listProgramManifestsForProblemSQL)
      try
        statement.setObject(1, problemId.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readProgramManifest(resultSet, "program_manifest"))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  private def bindGlobalListFilterStatement(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    request: SubmissionListRequest,
    includeDetailVisibility: Boolean,
    hasGlobalViewOverride: Boolean
  ): Int =
    val afterDetailVisibility =
      if includeDetailVisibility then bindVisibility(statement, 1, actor, hasGlobalViewOverride)
      else 1

    val afterGlobalSourceVisibility = bindVisibleContest(statement, afterDetailVisibility, actor)
    val afterSummaryVisibility = bindVisibility(statement, afterGlobalSourceVisibility, actor, hasGlobalViewOverride)
    bindCommonListFilters(statement, afterSummaryVisibility, request)

  private def bindContestListFilterStatement(
    statement: PreparedStatement,
    actor: AuthenticatedUser,
    contestId: ContestId,
    request: SubmissionListRequest,
    includeDetailVisibility: Boolean,
    canViewAllContestSubmissions: Boolean
  ): Int =
    val afterDetailVisibility =
      if includeDetailVisibility then bindContestSubmissionOwner(statement, 1, actor, canViewAllContestSubmissions)
      else 1

    statement.setObject(afterDetailVisibility, contestId.value)
    val afterContestId = afterDetailVisibility + 1
    val afterSubmitter = bindContestSubmissionOwner(statement, afterContestId, actor, canViewAllContestSubmissions)
    bindCommonListFilters(statement, afterSubmitter, request)

  private def bindCommonListFilters(
    statement: PreparedStatement,
    startIndex: Int,
    request: SubmissionListRequest
  ): Int =
    val afterUserQuery = bindUserQuery(statement, startIndex, request.userQuery)
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
    actor: AuthenticatedUser,
    hasGlobalViewOverride: Boolean
  ): Int =
    val afterProblemManager = bindProblemManagerVisibility(statement, startIndex, actor)
    val afterOrdinary = bindOrdinaryVisibility(statement, afterProblemManager, actor, hasGlobalViewOverride)
    bindVisibleContest(statement, afterOrdinary, actor)

  private def bindContestSubmissionOwner(
    statement: PreparedStatement,
    startIndex: Int,
    actor: AuthenticatedUser,
    canViewAllContestSubmissions: Boolean
  ): Int =
    val afterCanViewAll = bindBoolean(statement, startIndex, canViewAllContestSubmissions)
    bindString(statement, afterCanViewAll, actor.username.value)

  private def bindProblemManagerVisibility(
    statement: PreparedStatement,
    startIndex: Int,
    actor: AuthenticatedUser
  ): Int =
    val afterGlobalProblemManager = bindBoolean(statement, startIndex, actor.siteManager || actor.problemManager)
    val afterUserManagerGrant = bindString(statement, afterGlobalProblemManager, actor.username.value)
    bindString(statement, afterUserManagerGrant, actor.username.value)

  private def bindOrdinaryVisibility(
    statement: PreparedStatement,
    startIndex: Int,
    actor: AuthenticatedUser,
    hasGlobalViewOverride: Boolean
  ): Int =
    val afterGlobalOverride = bindBoolean(statement, startIndex, hasGlobalViewOverride)
    val afterOwnUsername = bindString(statement, afterGlobalOverride, actor.username.value)
    val afterProblemViewerGrant = bindString(statement, afterOwnUsername, actor.username.value)
    val afterProblemGroupGrant = bindString(statement, afterProblemViewerGrant, actor.username.value)
    val afterProblemSetOverride = bindBoolean(statement, afterProblemGroupGrant, hasGlobalViewOverride)
    val afterProblemSetViewerGrant = bindString(statement, afterProblemSetOverride, actor.username.value)
    bindString(statement, afterProblemSetViewerGrant, actor.username.value)

  private def bindVisibleContest(
    statement: PreparedStatement,
    startIndex: Int,
    actor: AuthenticatedUser
  ): Int =
    val afterGlobalOverride = bindBoolean(statement, startIndex, actor.siteManager || actor.contestManager)
    val afterUserGrant = bindString(statement, afterGlobalOverride, actor.username.value)
    val afterGroupGrant = bindString(statement, afterUserGrant, actor.username.value)
    bindString(statement, afterGroupGrant, actor.username.value)

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
