package domains.auth.objects.response

import domains.auth.objects.{AuthPermissionFlags, EmailAddress}
import domains.user.objects.UserProfileSettings

import domains.user.objects.{DisplayName, UserAvatarUrl, UserPreferences, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 当前会话响应，返回登录用户资料、邮箱、偏好和权限状态。 */
final case class SessionResponse(
  displayName: DisplayName,
  username: Username,
  avatarUrl: Option[UserAvatarUrl],
  email: EmailAddress,
  preferences: UserPreferences,
  siteManager: Boolean,
  problemManager: Boolean,
  contestManager: Boolean
)

/** 提供会话响应编解码和从账号/资料片段组装响应的入口。 */
object SessionResponse:
  given Encoder[SessionResponse] = deriveEncoder[SessionResponse]
  given Decoder[SessionResponse] = deriveDecoder[SessionResponse]

  /** 组合用户资料、账号邮箱和权限，归一化权限后生成当前会话响应。 */
  def fromParts(
    profile: UserProfileSettings,
    email: EmailAddress,
    siteManager: Boolean,
    problemManager: Boolean,
    contestManager: Boolean
  ): SessionResponse =
    val permissions = AuthPermissionFlags.normalize(siteManager, problemManager, contestManager)
    SessionResponse(
      displayName = profile.displayName,
      username = profile.username,
      avatarUrl = profile.avatarUrl,
      email = email,
      preferences = profile.preferences,
      siteManager = permissions.siteManager,
      problemManager = permissions.problemManager,
      contestManager = permissions.contestManager
    )
