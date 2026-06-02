package domains.user.table.user_profile



import cats.effect.IO
import domains.user.objects.{DisplayName, Username}
import domains.user.objects.internal.UserAvatarRecord
import domains.problem.objects.ProblemTitleDisplayMode
import domains.user.objects.UserProfileSettings
import domains.user.objects.response.UserSettingsResponse
import domains.user.objects.{UserDisplayMode, UserLocale}
import domains.user.table.user_profile.UserProfileTableSupport.*

import java.sql.{Connection, Timestamp}

object UserProfileTable:

  def initialize(connection: Connection): IO[Unit] =
    UserProfileTableSchema.initializeSchema(connection)

  private val findSettingsByUsernameSQL: String =
    """
      |select username, display_name, display_mode, locale, problem_title_display_mode, auto_mark_message_read
      |     , avatar_object_key, avatar_content_type, avatar_updated_at
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
      |       up.avatar_object_key, up.avatar_content_type, up.avatar_updated_at,
      |       aa.email, aa.site_manager, aa.problem_manager, aa.contest_manager
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
      |returning username, display_name, display_mode, locale, problem_title_display_mode, auto_mark_message_read,
      |          avatar_object_key, avatar_content_type, avatar_updated_at
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
      |returning username, display_name, display_mode, locale, problem_title_display_mode, auto_mark_message_read,
      |          avatar_object_key, avatar_content_type, avatar_updated_at
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

  private val findAvatarByUsernameSQL: String =
    """
      |select username, avatar_object_key, avatar_content_type, avatar_updated_at
      |from user_profiles
      |where lower(username) = lower(?)
      |""".stripMargin

  def findAvatarByUsername(connection: Connection, username: Username): IO[Option[UserAvatarRecord]] =
    IO.blocking {
      val statement = connection.prepareStatement(findAvatarByUsernameSQL)
      try
        statement.setString(1, username.value.trim)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then readAvatarRecord(resultSet)
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val updateAvatarSQL: String =
    """
      |update user_profiles
      |set avatar_object_key = ?, avatar_content_type = ?, avatar_updated_at = ?
      |where username = ?
      |returning username, avatar_object_key, avatar_content_type, avatar_updated_at
      |""".stripMargin

  def updateAvatar(
    connection: Connection,
    username: Username,
    objectKey: String,
    contentType: String,
    updatedAt: java.time.Instant
  ): IO[Option[UserAvatarRecord]] =
    IO.blocking {
      val statement = connection.prepareStatement(updateAvatarSQL)
      try
        statement.setString(1, objectKey)
        statement.setString(2, contentType)
        statement.setTimestamp(3, Timestamp.from(updatedAt))
        statement.setString(4, username.value.trim)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then readAvatarRecord(resultSet)
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val clearAvatarSQL: String =
    """
      |update user_profiles
      |set avatar_object_key = null, avatar_content_type = null, avatar_updated_at = null
      |where username = ?
      |returning username
      |""".stripMargin

  def clearAvatar(connection: Connection, username: Username): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(clearAvatarSQL)
      try
        statement.setString(1, username.value.trim)
        val resultSet = statement.executeQuery()
        try resultSet.next()
        finally resultSet.close()
      finally statement.close()
    }
