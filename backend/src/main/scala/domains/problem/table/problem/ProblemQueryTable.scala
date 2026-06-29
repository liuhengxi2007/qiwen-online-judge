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
import domains.submission.objects.SubmissionResultDisplayMode
import shared.objects.PageResponse
import shared.objects.access.GrantRole

import java.sql.Connection

/** problems 表的读取入口；封装题目可见性、管理建议、详情读取和题单包含可见性查询。 */
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

  private val accessPredicate: String =
    s"""
      |(
      |  $managerAccessPredicate
      |  or $normalAccessPredicate
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

  /** 按访问策略分页列出调用者可见题目；输出摘要会补齐访问策略 grants。 */
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

  /** 按 slug 读取题目详情并补齐授权策略；不做权限过滤。 */
  def findBySlug(connection: Connection, slug: ProblemSlug): IO[Option[ProblemDetail]] =
    findBySlugUsing(connection, slug, findBySlugSQL)

  private val findResultDisplayModeByIdSQL: String =
    """
      |select result_display_mode
      |from problems
      |where id = ?
      |""".stripMargin

  /** 按题目 id 读取提交结果展示模式；供提交创建时固化展示策略。 */
  def findResultDisplayModeById(connection: Connection, problemId: ProblemId): IO[Option[SubmissionResultDisplayMode]] =
    IO.blocking {
      val statement = connection.prepareStatement(findResultDisplayModeByIdSQL)
      try
        statement.setObject(1, problemId.value)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            Some(parseColumn("problems.result_display_mode", resultSet.getString("result_display_mode"), SubmissionResultDisplayMode.parse))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val findBySlugForUpdateSQL: String =
    findBySlugSQL + "\nfor update of p"

  /** 按 slug 读取题目详情并加行锁；用于数据写入和 ready 状态更新。 */
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

  /** 返回调用者可见题目的搜索建议，按精确/前缀/包含匹配排序。 */
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

  private val listManageableSuggestionsSQL: String =
    s"""
      |select p.slug, p.title
      |from problems p
      |where
      |  $managerAccessPredicate
      |  and $searchPredicate
      |order by
      |  $suggestionOrderClause
      |limit $suggestionLimit
      |""".stripMargin

  /** 返回调用者可管理题目的搜索建议，使用管理权限谓词过滤。 */
  def listManageableSuggestions(connection: Connection, actor: AuthenticatedUser, query: ProblemSearchQuery): IO[List[ProblemSuggestion]] =
    IO.blocking {
      val statement = connection.prepareStatement(listManageableSuggestionsSQL)
      try
        bindManageableSuggestionQuery(statement, actor, query)
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

  /** 判断是否存在调用者可见且包含该题目的题单；用于题目继承可见性。 */
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
