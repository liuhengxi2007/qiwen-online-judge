package domains.user.api

import cats.effect.IO
import domains.problem.objects.ProblemTitleDisplayMode
import domains.user.objects.{DisplayName, UserDisplayMode, UserLocale, Username}
import domains.user.objects.internal.UserProfileSettings
import domains.user.table.user.UserTable

import java.sql.Connection

object UserProfileRecords:

  def create(
    connection: Connection,
    username: Username,
    displayName: DisplayName,
    displayMode: UserDisplayMode,
    locale: UserLocale,
    problemTitleDisplayMode: ProblemTitleDisplayMode,
    autoMarkMessageRead: Boolean
  ): IO[UserProfileSettings] =
    UserTable.insertProfile(
      connection,
      username = username,
      displayName = displayName,
      displayMode = displayMode,
      locale = locale,
      problemTitleDisplayMode = problemTitleDisplayMode,
      autoMarkMessageRead = autoMarkMessageRead
    )

  def findSettings(connection: Connection, username: Username): IO[Option[UserProfileSettings]] =
    UserTable.findSettingsByUsername(connection, username)
