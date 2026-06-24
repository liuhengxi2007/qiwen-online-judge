package domains.user.table.user_profile

import cats.effect.IO
import database.utils.LikePatternSql
import domains.auth.objects.SiteManagerUser
import domains.user.objects.request.{UserListRequest, UserSearchQuery}
import domains.user.objects.response.{UserAcceptedRanklistItem, UserContributionRanklistItem, UserListResponse}
import domains.user.objects.{UserAcceptedProblem, UserIdentity, Username}
import domains.user.table.user_profile.UserProfileTableSupport.*
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection

/** 用户资料查询表层，负责管理列表、建议、排行榜和公开资料聚合查询。 */
object UserProfileQueryTable:

  private def searchPredicate(usernameColumn: String, displayNameColumn: String): String =
    s"(? = false or lower($usernameColumn) like lower(?) escape '\\' or lower($displayNameColumn) like lower(?) escape '\\')"

  private val listUsersSQL: String =
    s"""
      |select aa.username, up.display_name, aa.email, aa.site_manager, aa.problem_manager, aa.contest_manager
      |from auth_accounts aa
      |join user_profiles up on up.username = aa.username
      |where ${searchPredicate("aa.username", "up.display_name")}
      |order by aa.username asc
      |limit ? offset ?
      |""".stripMargin

  private val countUsersSQL: String =
    s"""
      |select count(*) as total_items
      |from auth_accounts aa
      |join user_profiles up on up.username = aa.username
      |where ${searchPredicate("aa.username", "up.display_name")}
      |""".stripMargin

  /** 查询管理端用户列表，按搜索词过滤并返回分页结果；调用者必须已校验站点管理员。 */
  def listUsers(connection: Connection, actor: SiteManagerUser, request: UserListRequest): IO[UserListResponse] =
    val _ = actor
    val normalizedPageRequest = request.pageRequest.normalized
    val normalizedRequest = request.copy(pageRequest = normalizedPageRequest)
    for
      totalItems <- IO.blocking {
        val statement = connection.prepareStatement(countUsersSQL)
        try
          bindUserSearchQuery(statement, normalizedRequest.query, startIndex = 1)
          val resultSet = statement.executeQuery()
          try
            if resultSet.next() then resultSet.getLong("total_items")
            else 0L
          finally resultSet.close()
        finally statement.close()
      }
      items <- IO.blocking {
        val statement = connection.prepareStatement(listUsersSQL)
        try
          val nextIndex = bindUserSearchQuery(statement, normalizedRequest.query, startIndex = 1)
          statement.setInt(nextIndex, normalizedRequest.pageRequest.pageSize)
          statement.setInt(nextIndex + 1, (normalizedRequest.pageRequest.page - 1) * normalizedRequest.pageRequest.pageSize)
          val resultSet = statement.executeQuery()
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ => readUserListItem(resultSet))
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

  private val suggestionLimit: Int = 5

  private val listSuggestionsSQL: String =
    s"""
      |select up.username as submitter_username,
      |       up.display_name as submitter_display_name
      |from user_profiles up
      |where ${searchPredicate("up.username", "up.display_name")}
      |order by
      |  case
      |    when lower(up.username) = lower(?) then 0
      |    when lower(up.username) like lower(?) escape '\' then 1
      |    when lower(up.display_name) like lower(?) escape '\' then 2
      |    when lower(up.username) like lower(?) escape '\' then 3
      |    else 4
      |  end,
      |  lower(up.username) asc
      |limit $suggestionLimit
      |""".stripMargin

  /** 根据搜索词返回用户建议，优先精确和前缀匹配。 */
  def listSuggestions(connection: Connection, query: UserSearchQuery): IO[List[UserIdentity]] =
    IO.blocking {
      val statement = connection.prepareStatement(listSuggestionsSQL)
      try
        val searchPattern = LikePatternSql.fromRaw(query.value)
        val nextIndex = bindUserSearchQuery(statement, Some(query), startIndex = 1)
        statement.setString(nextIndex, searchPattern.raw)
        statement.setString(nextIndex + 1, searchPattern.prefixPattern)
        statement.setString(nextIndex + 2, searchPattern.prefixPattern)
        statement.setString(nextIndex + 3, searchPattern.containsPattern)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readUserIdentity(resultSet, "submitter"))
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  private val listContributionRanklistSQL: String =
    """
      |with blog_scores as (
      |  select b.author_username,
      |         sum(case when bv.vote = 'up' then 1 when bv.vote = 'down' then -1 else 0 end)::numeric as blog_score
      |  from blogs b
      |  left join blog_votes bv on bv.blog_id = b.id
      |  group by b.author_username
      |),
      |comment_scores as (
      |  select c.author_username,
      |         sum(case when bcv.vote = 'up' then 1 when bcv.vote = 'down' then -1 else 0 end)::numeric as comment_score
      |  from blog_comments c
      |  left join blog_comment_votes bcv on bcv.comment_id = c.id
      |  group by c.author_username
      |)
      |select aa.username,
      |       up.display_name,
      |       round(coalesce(blog_scores.blog_score, 0)::numeric + coalesce(comment_scores.comment_score, 0)::numeric * 0.1) as contribution
      |from auth_accounts aa
      |join user_profiles up on up.username = aa.username
      |left join blog_scores on blog_scores.author_username = aa.username
      |left join comment_scores on comment_scores.author_username = aa.username
      |order by contribution desc, lower(up.display_name) asc, lower(aa.username) asc
      |limit ? offset ?
      |""".stripMargin

  /** 查询贡献排行榜分页，贡献由博客和评论投票聚合计算。 */
  def listContributionRanklist(connection: Connection, pageRequest: PageRequest): IO[PageResponse[UserContributionRanklistItem]] =
    IO.blocking {
      val normalizedPageRequest = pageRequest.normalized
      val totalItems = countAllUsers(connection)
      val statement = connection.prepareStatement(listContributionRanklistSQL)
      try
        statement.setInt(1, normalizedPageRequest.pageSize)
        statement.setInt(2, (normalizedPageRequest.page - 1) * normalizedPageRequest.pageSize)
        val resultSet = statement.executeQuery()
        try
          val items = Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readContributionRanklistItem(resultSet))
            .toList

          PageResponse(
            items = items,
            page = normalizedPageRequest.page,
            pageSize = normalizedPageRequest.pageSize,
            totalItems = totalItems
          )
        finally resultSet.close()
      finally statement.close()
    }

  private val listAcceptedRanklistSQL: String =
    """
      |with accepted_counts as (
      |  select lower(s.submitter_username) as submitter_username,
      |         count(distinct s.problem_id)::int as accepted_count
      |  from submissions s
      |  where s.verdict = 'accepted'
      |  group by lower(s.submitter_username)
      |)
      |select aa.username,
      |       up.display_name,
      |       coalesce(accepted_counts.accepted_count, 0) as accepted_count
      |from auth_accounts aa
      |join user_profiles up on up.username = aa.username
      |left join accepted_counts on accepted_counts.submitter_username = lower(aa.username)
      |order by accepted_count desc, lower(up.display_name) asc, lower(aa.username) asc
      |limit ? offset ?
      |""".stripMargin

  /** 查询 AC 题数排行榜分页，按不同 accepted 题目数量排序。 */
  def listAcceptedRanklist(connection: Connection, pageRequest: PageRequest): IO[PageResponse[UserAcceptedRanklistItem]] =
    IO.blocking {
      val normalizedPageRequest = pageRequest.normalized
      val totalItems = countAllUsers(connection)
      val statement = connection.prepareStatement(listAcceptedRanklistSQL)
      try
        statement.setInt(1, normalizedPageRequest.pageSize)
        statement.setInt(2, (normalizedPageRequest.page - 1) * normalizedPageRequest.pageSize)
        val resultSet = statement.executeQuery()
        try
          val items = Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readAcceptedRanklistItem(resultSet))
            .toList

          PageResponse(
            items = items,
            page = normalizedPageRequest.page,
            pageSize = normalizedPageRequest.pageSize,
            totalItems = totalItems
          )
        finally resultSet.close()
      finally statement.close()
    }

  private val countAcceptedProblemsSQL: String =
    """
      |select count(distinct s.problem_id)::int as accepted_count
      |from submissions s
      |where lower(s.submitter_username) = lower(?)
      |  and s.verdict = 'accepted'
      |""".stripMargin

  /** 查询指定用户已通过的不同题目数量。 */
  def countAcceptedProblems(connection: Connection, username: Username): IO[Int] =
    IO.blocking {
      val statement = connection.prepareStatement(countAcceptedProblemsSQL)
      try
        statement.setString(1, username.value)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then resultSet.getInt("accepted_count")
          else 0
        finally resultSet.close()
      finally statement.close()
    }

  private val listAcceptedProblemsSQL: String =
    """
      |with accepted_problem_times as (
      |  select s.problem_id,
      |         max(coalesce(s.finished_at, s.submitted_at)) as accepted_at
      |  from submissions s
      |  where lower(s.submitter_username) = lower(?)
      |    and s.verdict = 'accepted'
      |  group by s.problem_id
      |)
      |select p.slug,
      |       p.title,
      |       accepted_problem_times.accepted_at
      |from accepted_problem_times
      |join problems p on p.id = accepted_problem_times.problem_id
      |order by accepted_problem_times.accepted_at desc, p.slug asc
      |limit ? offset ?
      |""".stripMargin

  /** 查询指定用户已通过的题目分页列表，按最近通过时间倒序。 */
  def listAcceptedProblems(connection: Connection, username: Username, pageRequest: PageRequest): IO[PageResponse[UserAcceptedProblem]] =
    val normalizedPageRequest = pageRequest.normalized
    for
      totalItems <- countAcceptedProblems(connection, username).map(_.toLong)
      items <- IO.blocking {
        val statement = connection.prepareStatement(listAcceptedProblemsSQL)
        try
          statement.setString(1, username.value)
          statement.setInt(2, normalizedPageRequest.pageSize)
          statement.setInt(3, (normalizedPageRequest.page - 1) * normalizedPageRequest.pageSize)
          val resultSet = statement.executeQuery()
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ => readAcceptedProblem(resultSet))
              .toList
          finally resultSet.close()
        finally statement.close()
      }
    yield PageResponse(
      items = items,
      page = normalizedPageRequest.page,
      pageSize = normalizedPageRequest.pageSize,
      totalItems = totalItems
    )

  private val countAllUsersSQL: String =
    """
      |select count(*) as total_items
      |from auth_accounts
      |""".stripMargin

  private def countAllUsers(connection: Connection): Long =
    val statement = connection.prepareStatement(countAllUsersSQL)
    try
      val resultSet = statement.executeQuery()
      try
        if resultSet.next() then resultSet.getLong("total_items")
        else 0L
      finally resultSet.close()
    finally statement.close()
