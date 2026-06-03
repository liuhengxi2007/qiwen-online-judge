package domains.problem.table.problem

import cats.effect.IO
import cats.syntax.all.*
import database.utils.UserIdentitySql
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.objects.{ProblemId, ProblemSlug}
import domains.problem.objects.request.{ProblemListRequest, ProblemSearchQuery}
import domains.problem.objects.response.{ProblemDetail, ProblemSuggestion, ProblemSummary}
import domains.problem.table.problem_access_grant.ProblemAccessGrantTable
import domains.problem.table.problem.ProblemTableSupport.*
import shared.objects.PageResponse
import shared.objects.access.GrantRole

import java.sql.Connection

object ProblemQueryTable:

  private val normalAccessPredicate: String =
    """
      |(
      |  ? = true
      |  or p.base_access = 'public'
      |  or exists (
      |    select 1
      |    from problem_access_grants pag
      |    where pag.problem_id = p.id
      |      and pag.grant_role = 'viewer'
      |      and pag.subject_kind = 'user'
      |      and pag.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from problem_access_grants pag
      |    join user_groups ug on ug.slug = pag.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where pag.problem_id = p.id
      |      and pag.grant_role = 'viewer'
      |      and pag.subject_kind = 'user_group'
      |      and ugm.username = ?
      |  )
      |  or exists (
      |    select 1
      |    from problem_set_problems psp
      |    join problem_sets ps on ps.id = psp.problem_set_id
      |    where psp.problem_id = p.id
      |      and (
      |        ? = true
      |        or ps.base_access = 'public'
      |        or exists (
      |          select 1
      |          from problem_set_access_grants psag
      |          where psag.problem_set_id = ps.id
      |            and psag.grant_role = 'viewer'
      |            and psag.subject_kind = 'user'
      |            and psag.subject_key = ?
      |        )
      |        or exists (
      |          select 1
      |          from problem_set_access_grants psag
      |          join user_groups ug on ug.slug = psag.subject_key
      |          join user_group_memberships ugm on ugm.user_group_id = ug.id
      |          where psag.problem_set_id = ps.id
      |            and psag.grant_role = 'viewer'
      |            and psag.subject_kind = 'user_group'
      |            and ugm.username = ?
      |        )
      |      )
      |  )
      |)
      |""".stripMargin

  private val managerAccessPredicate: String =
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

  private val visibleUnfinishedContestPredicate: String =
    s"""
      |exists (
      |    select 1
      |    from contest_problems cp
      |    join contests c on c.id = cp.contest_id
      |    where cp.problem_id = p.id
      |      and c.end_at >= now()
      |      and $visibleContestPredicate
      |)
      |""".stripMargin

  private val visibleEndedContestPredicate: String =
    s"""
      |exists (
      |  select 1
      |  from contest_problems cp
      |  join contests c on c.id = cp.contest_id
      |  where cp.problem_id = p.id
      |    and c.end_at < now()
      |    and $visibleContestPredicate
      |)
      |""".stripMargin

  private val accessPredicate: String =
    s"""
      |(
      |  $managerAccessPredicate
      |  or (
      |    not $visibleUnfinishedContestPredicate
      |    and (
      |      $normalAccessPredicate
      |      or $visibleEndedContestPredicate
      |    )
      |  )
      |)
      |""".stripMargin

  private val searchPredicate: String =
    """
      |(? = false or lower(p.slug) like lower(?) escape '\' or lower(p.title) like lower(?) escape '\')
      |""".stripMargin

  private val listSQL: String =
    s"""
      |select p.id, p.slug, p.title, p.data_name, p.ready, p.base_access, p.other_user_submission_access, ${UserIdentitySql.selectOptionalColumns("p.author_username", "author", "au")}, p.created_at, p.updated_at
      |from problems p
      |${UserIdentitySql.leftJoinUserProfiles("p.author_username", "au")}
      |where
      |  $accessPredicate
      |  and $searchPredicate
      |order by p.updated_at desc, p.slug asc
      |limit ? offset ?
      |""".stripMargin

  private val countSQL: String =
    s"""
      |select count(*) as total_items
      |from problems p
      |where
      |  $accessPredicate
      |  and $searchPredicate
      |""".stripMargin

  def listVisibleTo(connection: Connection, actor: AuthenticatedUser, request: ProblemListRequest): IO[PageResponse[ProblemSummary]] =
    for
      totalItems <- IO.blocking {
        val statement = connection.prepareStatement(countSQL)
        try
          bindListQuery(statement, actor, request.query, pageSize = None, offset = None)
          val resultSet = statement.executeQuery()
          try if resultSet.next() then resultSet.getLong("total_items") else 0L
          finally resultSet.close()
        finally statement.close()
      }
      items <- IO.blocking {
        val statement = connection.prepareStatement(listSQL)
        try
          bindListQuery(
            statement,
            actor,
            request.query,
            pageSize = Some(request.pageRequest.pageSize),
            offset = Some((request.pageRequest.page - 1) * request.pageRequest.pageSize)
          )
          val resultSet = statement.executeQuery()
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ => readProblemSummaryBase(resultSet))
              .toList
          finally resultSet.close()
        finally statement.close()
      }
      itemsWithPolicies <- items.traverse { item =>
        for
          viewerGrants <- ProblemAccessGrantTable.listForProblem(connection, item.id, GrantRole.Viewer)
          managerGrants <- ProblemAccessGrantTable.listForProblem(connection, item.id, GrantRole.Manager)
        yield item.copy(accessPolicy = item.accessPolicy.copy(viewerGrants = viewerGrants, managerGrants = managerGrants))
      }
    yield PageResponse(
      items = itemsWithPolicies,
      page = request.pageRequest.page,
      pageSize = request.pageRequest.pageSize,
      totalItems = totalItems
    )

  private val findBySlugSQL: String =
    s"""
      |select p.id, p.slug, p.title, p.statement_text, p.data_name, p.ready, p.base_access, p.other_user_submission_access, ${UserIdentitySql.selectOptionalColumns("p.author_username", "author", "au")}, p.created_at, p.updated_at
      |from problems p
      |${UserIdentitySql.leftJoinUserProfiles("p.author_username", "au")}
      |where p.slug = ?
      |""".stripMargin

  def findBySlug(connection: Connection, slug: ProblemSlug): IO[Option[ProblemDetail]] =
    findBySlugUsing(connection, slug, findBySlugSQL)

  private val findBySlugForUpdateSQL: String =
    findBySlugSQL + "\nfor update of p"

  def findBySlugForUpdate(connection: Connection, slug: ProblemSlug): IO[Option[ProblemDetail]] =
    findBySlugUsing(connection, slug, findBySlugForUpdateSQL)

  private def findBySlugUsing(connection: Connection, slug: ProblemSlug, sql: String): IO[Option[ProblemDetail]] =
    IO.blocking {
      val statement = connection.prepareStatement(sql)
      try
        statement.setString(1, slug.value)
        val resultSet = statement.executeQuery()
        try if resultSet.next() then Some(readProblemDetailBase(resultSet)) else None
        finally resultSet.close()
      finally statement.close()
    }.flatMap {
      case Some(problem) =>
        for
          viewerGrants <- ProblemAccessGrantTable.listForProblem(connection, problem.id, GrantRole.Viewer)
          managerGrants <- ProblemAccessGrantTable.listForProblem(connection, problem.id, GrantRole.Manager)
        yield Some(problem.copy(accessPolicy = problem.accessPolicy.copy(viewerGrants = viewerGrants, managerGrants = managerGrants)))
      case None =>
        IO.pure(None)
    }

  private val suggestionLimit: Int = 5

  private val suggestionOrderClause: String =
    """
      |case
      |  when lower(p.slug) = lower(?) then 0
      |  when lower(p.slug) like lower(?) escape '\' then 1
      |  when lower(p.title) like lower(?) escape '\' then 2
      |  when lower(p.slug) like lower(?) escape '\' then 3
      |  else 4
      |end,
      |p.slug asc
      |""".stripMargin

  private val listSuggestionsSQL: String =
    s"""
      |select p.slug, p.title
      |from problems p
      |where
      |  $accessPredicate
      |  and $searchPredicate
      |order by
      |  $suggestionOrderClause
      |limit $suggestionLimit
      |""".stripMargin

  def listSuggestions(connection: Connection, actor: AuthenticatedUser, query: ProblemSearchQuery): IO[List[ProblemSuggestion]] =
    IO.blocking {
      val statement = connection.prepareStatement(listSuggestionsSQL)
      try
        bindSuggestionQuery(statement, actor, query)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readProblemSuggestion(resultSet))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  private val hasVisibleContainingProblemSetSQL: String =
    """
      |select 1
      |from problem_set_problems psp
      |join problem_sets ps on ps.id = psp.problem_set_id
      |where psp.problem_id = ?
      |  and (
      |    ? = true
      |    or ps.base_access = 'public'
      |    or exists (
      |      select 1
      |      from problem_set_access_grants psag
      |      where psag.problem_set_id = ps.id
      |        and psag.grant_role = 'viewer'
      |        and psag.subject_kind = 'user'
      |        and psag.subject_key = ?
      |    )
      |    or exists (
      |      select 1
      |      from problem_set_access_grants psag
      |      join user_groups ug on ug.slug = psag.subject_key
      |      join user_group_memberships ugm on ugm.user_group_id = ug.id
      |      where psag.problem_set_id = ps.id
      |        and psag.grant_role = 'viewer'
      |        and psag.subject_kind = 'user_group'
      |        and ugm.username = ?
      |    )
      |  )
      |limit 1
      |""".stripMargin

  def hasVisibleContainingProblemSet(
    connection: Connection,
    actor: AuthenticatedUser,
    problemId: ProblemId
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(hasVisibleContainingProblemSetSQL)
      try
        bindContainingProblemSetAccessQuery(statement, actor, problemId)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }
