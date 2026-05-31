package domains.user.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.user.objects.Username
import domains.user.objects.internal.UserProfileSettings
import domains.user.table.user_profile.UserProfileTable
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

object FindUserProfileSettings extends InternalOnlyApi[Username, Option[UserProfileSettings]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/internal/users/:username/profile-settings")

  override def plan(connection: Connection, username: Username): IO[Option[UserProfileSettings]] =
    UserProfileTable.findSettingsByUsername(connection, username)
