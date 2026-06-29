package domains.user.table.user_profile



import cats.effect.IO
import domains.user.objects.{DisplayName, Username}
import domains.problem.objects.ProblemTitleDisplayMode
import domains.user.objects.UserProfileSettings
import domains.user.objects.response.UserSettingsResponse
import domains.user.objects.{UserDisplayMode, UserLocale}
import domains.user.table.user_profile.UserProfileTableSupport.*

import java.sql.{Connection, Timestamp}

/** user_profiles 表访问层，负责用户资料、偏好和头像元数据的持久化。 */
object UserProfileTable:

  /** 初始化用户资料表结构并从旧账号表回填资料字段。 */
  def initialize(connection: Connection): IO[Unit] =
    UserProfileTableSchema.initializeSchema(connection)

  private val findSettingsByUsernameSQL: String =
    """
      |select username, display_name, display_mode, locale, problem_title_display_mode, auto_mark_message_read
      |     , avatar_object_key, avatar_content_type, avatar_updated_at
      |from user_profiles
      |where lower(username) = lower(?)
      |""".stripMargin

  /** 按用户名大小写不敏感读取资料设置，包含可选头像 URL。 */
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

  /** 读取设置页所需的资料、邮箱和权限组合，用户不存在时返回 None。 */
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

  /** 插入用户资料初始记录，返回插入后的资料设置。 */
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

  /** 更新用户资料和偏好字段，目标用户不存在时返回 None。 */
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
      |select avatar_object_key, avatar_content_type, avatar_updated_at
      |from user_profiles
      |where lower(username) = lower(?)
      |""".stripMargin

  /** 读取头像对象 key 和内容类型；用户不存在或头像字段不完整时返回 None。 */
  def findAvatarByUsername(connection: Connection, username: Username): IO[Option[(String, String)]] =
    IO.blocking {
      val statement = connection.prepareStatement(findAvatarByUsernameSQL)
      try
        statement.setString(1, username.value.trim)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then readAvatarObject(resultSet)
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val updateAvatarSQL: String =
    """
      |update user_profiles
      |set avatar_object_key = ?, avatar_content_type = ?, avatar_updated_at = ?
      |where username = ?
      |returning username
      |""".stripMargin

  /** 更新头像对象 key、内容类型和更新时间，返回是否匹配到用户记录。 */
  def updateAvatar(
    connection: Connection,
    username: Username,
    objectKey: String,
    contentType: String,
    updatedAt: java.time.Instant
  ): IO[Boolean] =
    IO.blocking {
      val statement = connection.prepareStatement(updateAvatarSQL)
      try
        statement.setString(1, objectKey)
        statement.setString(2, contentType)
        statement.setTimestamp(3, Timestamp.from(updatedAt))
        statement.setString(4, username.value.trim)
        val resultSet = statement.executeQuery()
        try resultSet.next()
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

  /** 清空用户头像元数据，返回是否匹配到用户记录。 */
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
