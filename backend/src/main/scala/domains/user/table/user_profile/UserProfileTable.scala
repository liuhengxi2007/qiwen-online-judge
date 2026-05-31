package domains.user.table.user_profile



import cats.effect.IO
import domains.user.objects.{DisplayName, Username}
import domains.problem.objects.ProblemTitleDisplayMode
import domains.user.objects.internal.UserProfileSettings
import domains.user.objects.response.UserSettingsResponse
import domains.user.objects.{UserDisplayMode, UserLocale}
import domains.user.table.user_profile.UserProfileTableSupport.*

import java.sql.Connection

object UserProfileTable:

  def initialize(connection: Connection): IO[Unit] =
    UserProfileTableSchema.initializeSchema(connection)

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
      |       aa.email, aa.site_manager, aa.problem_manager
      |from user_profiles up
      |join auth_accounts aa on aa.username = up.username
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
