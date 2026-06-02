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

  private val listAcceptedProblemsSQL: String =
    """
      |select p.slug,
      |       p.title,
      |       max(coalesce(s.finished_at, s.submitted_at)) as accepted_at
      |from submissions s
      |join problems p on p.id = s.problem_id
      |where lower(s.submitter_username) = lower(?)
      |  and s.verdict = 'accepted'
      |group by p.slug, p.title
      |order by accepted_at desc, p.slug asc
      |""".stripMargin

  def listAcceptedProblems(connection: Connection, username: Username): IO[List[UserAcceptedProblem]] =
    IO.blocking {
      val statement = connection.prepareStatement(listAcceptedProblemsSQL)
      try
        statement.setString(1, username.value)
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
