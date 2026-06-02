package domains.user.api

import cats.effect.IO
import domains.auth.objects.internal.AuthenticatedUser
import domains.user.objects.Username
import domains.user.objects.response.UserSettingsResponse
import domains.user.table.user_profile.UserProfileTable
import shared.api.{ApiMessages, HttpApiError}

import java.sql.Connection

object UserAvatarApiHelpers:
  def ensureCanManageAvatar(actor: AuthenticatedUser, targetUsername: Username): IO[Unit] =
    HttpApiError.ensure(
      targetUsername.value == actor.username.value || actor.siteManager,
      HttpApiError.forbidden(ApiMessages.siteManagerRequired)
    )

  def refreshedSettings(connection: Connection, targetUsername: Username): IO[UserSettingsResponse] =
    UserProfileTable.findUserSettingsByUsername(connection, targetUsername).flatMap {
      case Some(settings) => IO.pure(settings)
      case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
    }
