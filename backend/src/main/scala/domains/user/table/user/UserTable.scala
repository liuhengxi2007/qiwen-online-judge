package domains.user.table.user



import cats.effect.IO
import domains.auth.model.{AuthUser, EmailAddress, PasswordHash, SiteManagerUser}
import domains.user.model.{DisplayName, Username}
import domains.problem.model.ProblemTitleDisplayMode
import shared.model.{PageRequest, PageResponse}
import database.utils.LikePatternSql
import domains.user.model.response.{UserAcceptedRanklistItem, UserListResponse, UserRanklistItem}
import domains.user.model.request.UserSearchQuery
import domains.user.model.{UserAcceptedProblem, UserDisplayMode, UserIdentity, UserLocale}
import domains.user.model.request.{UserListRequest}
import domains.user.table.user.UserTableSupport.*

import java.sql.{Connection, SQLException}

object UserTable:

  enum DeleteUserTableResult:
    case NotFound
    case Deleted
    case HasOwnedResources

  private val findByUsernameSQL: String =
    """
      |select username, display_name, email, display_mode, locale, problem_title_display_mode, auto_mark_message_read, password_hash, site_manager, problem_manager
      |from auth_users
      |where lower(username) = lower(?)
      |""".stripMargin

  def findByUsername(connection: Connection, username: Username): IO[Option[AuthUser]] =
    IO.blocking {
      val statement = connection.prepareStatement(findByUsernameSQL)
      try
        statement.setString(1, username.value.trim)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAuthUser(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val searchPredicate: String =
    """
      |(? = false or lower(username) like lower(?) escape '\' or lower(display_name) like lower(?) escape '\')
      |""".stripMargin

  private val listUsersSQL: String =
    s"""
      |select username, display_name, email, display_mode, locale, problem_title_display_mode, auto_mark_message_read, site_manager, problem_manager
      |from auth_users
      |where $searchPredicate
      |order by username asc
      |limit ? offset ?
      |""".stripMargin

  private val countUsersSQL: String =
    s"""
      |select count(*) as total_items
      |from auth_users
      |where $searchPredicate
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
      |select username as submitter_username,
      |       display_name as submitter_display_name
      |from auth_users
      |where $searchPredicate
      |order by
      |  case
      |    when lower(username) = lower(?) then 0
      |    when lower(username) like lower(?) escape '\' then 1
      |    when lower(display_name) like lower(?) escape '\' then 2
      |    when lower(username) like lower(?) escape '\' then 3
      |    else 4
      |  end,
      |  lower(username) asc
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
      |select au.username,
      |       au.display_name,
      |       round(coalesce(blog_scores.blog_score, 0)::numeric + coalesce(comment_scores.comment_score, 0)::numeric * 0.1) as contribution
      |from auth_users au
      |left join blog_scores on blog_scores.author_username = au.username
      |left join comment_scores on comment_scores.author_username = au.username
      |order by contribution desc, lower(au.display_name) asc, lower(au.username) asc
      |limit ? offset ?
      |""".stripMargin

  def listContributionRanklist(connection: Connection, pageRequest: PageRequest): IO[PageResponse[UserRanklistItem]] =
    IO.blocking {
      val normalizedPageRequest = pageRequest.normalized
      val totalItems = countUsers(connection)
      val statement = connection.prepareStatement(listContributionRanklistSQL)
      try
        statement.setInt(1, normalizedPageRequest.pageSize)
        statement.setInt(2, (normalizedPageRequest.page - 1) * normalizedPageRequest.pageSize)
        val resultSet = statement.executeQuery()
        try
          val items = Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => readRanklistItem(resultSet))
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
      |select au.username,
      |       au.display_name,
      |       coalesce(accepted_counts.accepted_count, 0) as accepted_count
      |from auth_users au
      |left join accepted_counts on accepted_counts.submitter_username = lower(au.username)
      |order by accepted_count desc, lower(au.display_name) asc, lower(au.username) asc
      |limit ? offset ?
      |""".stripMargin

  def listAcceptedRanklist(connection: Connection, pageRequest: PageRequest): IO[PageResponse[UserAcceptedRanklistItem]] =
    IO.blocking {
      val normalizedPageRequest = pageRequest.normalized
      val totalItems = countUsers(connection)
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

  private val updatePermissionsSQL: String =
    """
      |update auth_users
      |set site_manager = ?, problem_manager = ?
      |where username = ?
      |returning username, display_name, email, display_mode, locale, problem_title_display_mode, auto_mark_message_read, password_hash, site_manager, problem_manager
      |""".stripMargin

  def updatePermissions(
    connection: Connection,
    actor: SiteManagerUser,
    username: Username,
    siteManager: Boolean,
    problemManager: Boolean
  ): IO[Option[AuthUser]] =
    IO.blocking {
      val _ = actor
      val statement = connection.prepareStatement(updatePermissionsSQL)
      try
        statement.setBoolean(1, siteManager)
        statement.setBoolean(2, problemManager)
        statement.setString(3, username.value.trim)

        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAuthUser(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val updateSettingsSQL: String =
    """
      |update auth_users
      |set display_name = ?, email = ?, display_mode = ?, locale = ?, problem_title_display_mode = ?, auto_mark_message_read = ?, password_hash = ?
      |where username = ?
      |returning username, display_name, email, display_mode, locale, problem_title_display_mode, auto_mark_message_read, password_hash, site_manager, problem_manager
      |""".stripMargin

  def updateSettings(
    connection: Connection,
    username: Username,
    displayName: DisplayName,
    email: EmailAddress,
    displayMode: UserDisplayMode,
    locale: UserLocale,
    problemTitleDisplayMode: ProblemTitleDisplayMode,
    autoMarkMessageRead: Boolean,
    passwordHash: PasswordHash
  ): IO[Option[AuthUser]] =
    IO.blocking {
      val statement = connection.prepareStatement(updateSettingsSQL)
      try
        statement.setString(1, displayName.value.trim)
        statement.setString(2, email.value.trim)
        statement.setString(3, encodeUserDisplayModeColumn(displayMode))
        statement.setString(4, encodeUserLocaleColumn(locale))
        statement.setString(5, encodeProblemTitleDisplayModeColumn(problemTitleDisplayMode))
        statement.setBoolean(6, autoMarkMessageRead)
        statement.setString(7, passwordHash.value)
        statement.setString(8, username.value.trim)

        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAuthUser(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val deleteSQL: String =
    """
      |delete from auth_users
      |where username = ?
      |""".stripMargin

  def delete(connection: Connection, username: Username): IO[DeleteUserTableResult] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteSQL)
      try
        statement.setString(1, username.value)
        try
          val deletedRows = statement.executeUpdate()
          if deletedRows == 0 then DeleteUserTableResult.NotFound
          else DeleteUserTableResult.Deleted
        catch
          case exception: SQLException if exception.getSQLState == "23503" =>
            DeleteUserTableResult.HasOwnedResources
      finally statement.close()
    }

  private val countAllUsersSQL: String =
    """
      |select count(*) as total_items
      |from auth_users
      |""".stripMargin

  private def countUsers(connection: Connection): Long =
    val statement = connection.prepareStatement(countAllUsersSQL)
    try
      val resultSet = statement.executeQuery()
      try
        if resultSet.next() then resultSet.getLong("total_items")
        else 0L
      finally resultSet.close()
    finally statement.close()
