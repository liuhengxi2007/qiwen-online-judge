package domains.user.table.user



import cats.effect.IO
import domains.auth.objects.SiteManagerUser
import domains.user.objects.{DisplayName, Username}
import domains.problem.objects.ProblemTitleDisplayMode
import shared.objects.{PageRequest, PageResponse}
import database.utils.LikePatternSql
import domains.user.objects.internal.UserProfileSettings
import domains.user.objects.response.{UserAcceptedRanklistItem, UserListResponse, UserRanklistItem, UserSettingsResponse}
import domains.user.objects.request.UserSearchQuery
import domains.user.objects.{UserAcceptedProblem, UserDisplayMode, UserIdentity, UserLocale}
import domains.user.objects.request.{UserListRequest}
import domains.user.table.user.UserTableSupport.*

import java.sql.Connection

object UserTable:

  def initialize(connection: Connection): IO[Unit] =
    UserTableSchema.initializeSchema(connection)

  private val findSettingsByUsernameSQL: String =
    """
      |select username, display_name, display_mode, locale, problem_title_display_mode, auto_mark_message_read
      |from user_profiles
      |where lower(username) = lower(?)
      |""".stripMargin

  def findSettingsByUsername(connection: Connection, username: Username): IO[Option[UserProfileSettings]] =
    IO.blocking {
      val statement = connection.prepareStatement(findSettingsByUsernameSQL)
      try
        statement.setString(1, username.value.trim)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readProfileSettings(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val findUserSettingsByUsernameSQL: String =
    """
      |select up.username, up.display_name, up.display_mode, up.locale, up.problem_title_display_mode, up.auto_mark_message_read,
      |       au.email, au.site_manager, au.problem_manager
      |from user_profiles up
      |join auth_users au on au.username = up.username
      |where lower(up.username) = lower(?)
      |""".stripMargin

  def findUserSettingsByUsername(connection: Connection, username: Username): IO[Option[UserSettingsResponse]] =
    IO.blocking {
      val statement = connection.prepareStatement(findUserSettingsByUsernameSQL)
      try
        statement.setString(1, username.value.trim)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readUserSettingsResponse(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val insertProfileSQL: String =
    """
      |insert into user_profiles (username, display_name, display_mode, locale, problem_title_display_mode, auto_mark_message_read)
      |values (?, ?, ?, ?, ?, ?)
      |returning username, display_name, display_mode, locale, problem_title_display_mode, auto_mark_message_read
      |""".stripMargin

  def insertProfile(
    connection: Connection,
    username: Username,
    displayName: DisplayName,
    displayMode: UserDisplayMode,
    locale: UserLocale,
    problemTitleDisplayMode: ProblemTitleDisplayMode,
    autoMarkMessageRead: Boolean
  ): IO[UserProfileSettings] =
    IO.blocking {
      val statement = connection.prepareStatement(insertProfileSQL)
      try
        statement.setString(1, username.value.trim)
        statement.setString(2, displayName.value.trim)
        statement.setString(3, encodeUserDisplayModeColumn(displayMode))
        statement.setString(4, encodeUserLocaleColumn(locale))
        statement.setString(5, encodeProblemTitleDisplayModeColumn(problemTitleDisplayMode))
        statement.setBoolean(6, autoMarkMessageRead)

        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then readProfileSettings(resultSet)
          else throw new IllegalStateException("Insert succeeded but returned no user profile")
        finally resultSet.close()
      finally statement.close()
    }

  private def searchPredicate(usernameColumn: String, displayNameColumn: String): String =
    s"(? = false or lower($usernameColumn) like lower(?) escape '\\' or lower($displayNameColumn) like lower(?) escape '\\')"

  private val listUsersSQL: String =
    s"""
      |select au.username, up.display_name, au.email, au.site_manager, au.problem_manager
      |from auth_users au
      |join user_profiles up on up.username = au.username
      |where ${searchPredicate("au.username", "up.display_name")}
      |order by au.username asc
      |limit ? offset ?
      |""".stripMargin

  private val countUsersSQL: String =
    s"""
      |select count(*) as total_items
      |from auth_users au
      |join user_profiles up on up.username = au.username
      |where ${searchPredicate("au.username", "up.display_name")}
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
      |select au.username,
      |       up.display_name,
      |       round(coalesce(blog_scores.blog_score, 0)::numeric + coalesce(comment_scores.comment_score, 0)::numeric * 0.1) as contribution
      |from auth_users au
      |join user_profiles up on up.username = au.username
      |left join blog_scores on blog_scores.author_username = au.username
      |left join comment_scores on comment_scores.author_username = au.username
      |order by contribution desc, lower(up.display_name) asc, lower(au.username) asc
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
      |       up.display_name,
      |       coalesce(accepted_counts.accepted_count, 0) as accepted_count
      |from auth_users au
      |join user_profiles up on up.username = au.username
      |left join accepted_counts on accepted_counts.submitter_username = lower(au.username)
      |order by accepted_count desc, lower(up.display_name) asc, lower(au.username) asc
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

  private val updateSettingsSQL: String =
    """
      |update user_profiles
      |set display_name = ?, display_mode = ?, locale = ?, problem_title_display_mode = ?, auto_mark_message_read = ?
      |where username = ?
      |returning username, display_name, display_mode, locale, problem_title_display_mode, auto_mark_message_read
      |""".stripMargin

  def updateSettings(
    connection: Connection,
    username: Username,
    displayName: DisplayName,
    displayMode: UserDisplayMode,
    locale: UserLocale,
    problemTitleDisplayMode: ProblemTitleDisplayMode,
    autoMarkMessageRead: Boolean
  ): IO[Option[UserProfileSettings]] =
    IO.blocking {
      val statement = connection.prepareStatement(updateSettingsSQL)
      try
        statement.setString(1, displayName.value.trim)
        statement.setString(2, encodeUserDisplayModeColumn(displayMode))
        statement.setString(3, encodeUserLocaleColumn(locale))
        statement.setString(4, encodeProblemTitleDisplayModeColumn(problemTitleDisplayMode))
        statement.setBoolean(5, autoMarkMessageRead)
        statement.setString(6, username.value.trim)

        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readProfileSettings(resultSet))
          else None
        finally resultSet.close()
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
