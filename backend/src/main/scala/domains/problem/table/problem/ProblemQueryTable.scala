package domains.problem.table.problem

import cats.effect.IO
import cats.syntax.all.*
import database.table.resource_access_grant.ResourceAccessGrantTable
import database.utils.ResourceAccessTableSupport.policyFrom
import database.utils.UserIdentitySql
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.objects.{ProblemId, ProblemSlug}
import domains.problem.objects.request.{ProblemListRequest, ProblemSearchQuery}
import domains.problem.objects.response.{ProblemDetail, ProblemSuggestion, ProblemSummary}
import domains.problem.table.problem.ProblemTableSupport.*
import shared.objects.PageResponse
import shared.objects.access.{GrantRole, ResourceKind}

import java.sql.Connection

object ProblemQueryTable:

  private val accessPredicate: String =
    """
      |(
      |  ? = true
      |  or p.base_access = 'public'
      |  or exists (
      |    select 1
      |    from resource_access_grants rag
      |    where rag.resource_kind = 'problem'
      |      and rag.resource_id = p.id
      |      and rag.grant_role = 'viewer'
      |      and rag.subject_kind = 'user'
      |      and rag.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from resource_access_grants rag
      |    join user_groups ug on ug.slug = rag.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where rag.resource_kind = 'problem'
      |      and rag.resource_id = p.id
      |      and rag.grant_role = 'viewer'
      |      and rag.subject_kind = 'user_group'
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
      |          from resource_access_grants rag
      |          where rag.resource_kind = 'problem_set'
      |            and rag.resource_id = ps.id
      |            and rag.grant_role = 'viewer'
      |            and rag.subject_kind = 'user'
      |            and rag.subject_key = ?
      |        )
      |        or exists (
      |          select 1
      |          from resource_access_grants rag
      |          join user_groups ug on ug.slug = rag.subject_key
      |          join user_group_memberships ugm on ugm.user_group_id = ug.id
      |          where rag.resource_kind = 'problem_set'
      |            and rag.resource_id = ps.id
      |            and rag.grant_role = 'viewer'
      |            and rag.subject_kind = 'user_group'
      |            and ugm.username = ?
      |        )
      |      )
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
          viewerGrants <- ResourceAccessGrantTable.listForResource(connection, ResourceKind.Problem, toResourceId(item.id), GrantRole.Viewer)
          managerGrants <- ResourceAccessGrantTable.listForResource(connection, ResourceKind.Problem, toResourceId(item.id), GrantRole.Manager)
        yield item.copy(accessPolicy = policyFrom(item.accessPolicy.baseAccess, viewerGrants, managerGrants))
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
          viewerGrants <- ResourceAccessGrantTable.listForResource(connection, ResourceKind.Problem, toResourceId(problem.id), GrantRole.Viewer)
          managerGrants <- ResourceAccessGrantTable.listForResource(connection, ResourceKind.Problem, toResourceId(problem.id), GrantRole.Manager)
        yield Some(problem.copy(accessPolicy = policyFrom(problem.accessPolicy.baseAccess, viewerGrants, managerGrants)))
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
      |      from resource_access_grants rag
      |      where rag.resource_kind = 'problem_set'
      |        and rag.resource_id = ps.id
      |        and rag.grant_role = 'viewer'
      |        and rag.subject_kind = 'user'
      |        and rag.subject_key = ?
      |    )
      |    or exists (
      |      select 1
      |      from resource_access_grants rag
      |      join user_groups ug on ug.slug = rag.subject_key
      |      join user_group_memberships ugm on ugm.user_group_id = ug.id
      |      where rag.resource_kind = 'problem_set'
      |        and rag.resource_id = ps.id
      |        and rag.grant_role = 'viewer'
      |        and rag.subject_kind = 'user_group'
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
