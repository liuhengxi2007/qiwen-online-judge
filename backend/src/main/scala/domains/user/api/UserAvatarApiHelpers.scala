package domains.user.api

import cats.effect.IO
import domains.auth.objects.internal.AuthenticatedUser
import domains.user.objects.Username
import domains.user.objects.response.UserSettingsResponse
import domains.user.table.user_profile.UserProfileTable
import shared.api.{ApiMessages, HttpApiError}

import java.sql.Connection

/** 用户头像 API 共享辅助，集中权限校验和设置刷新逻辑；API 对齐例外：这是后端头像权限/刷新支持代码，不是前端端点。 */
object UserAvatarApiHelpers:
  /** 校验操作者可管理目标用户头像；仅本人或站点管理员允许。 */
  def ensureCanManageAvatar(actor: AuthenticatedUser, targetUsername: Username): IO[Unit] =
    HttpApiError.ensure(
      targetUsername.value == actor.username.value || actor.siteManager,
      HttpApiError.forbidden(ApiMessages.siteManagerRequired)
    )

  /** 头像变更后重新读取完整用户设置，用户不存在时返回 404。 */
  def refreshedSettings(connection: Connection, targetUsername: Username): IO[UserSettingsResponse] =
    UserProfileTable.findUserSettingsByUsername(connection, targetUsername).flatMap {
      case Some(settings) => IO.pure(settings)
      case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
    }
