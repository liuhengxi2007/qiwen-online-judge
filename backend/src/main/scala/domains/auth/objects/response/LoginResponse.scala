package domains.auth.objects.response

import domains.auth.objects.{AuthPermissionFlags, EmailAddress}
import domains.user.objects.UserProfileSettings

import domains.user.objects.{DisplayName, UserAvatarUrl, UserPreferences, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 登录成功响应，返回当前用户资料、邮箱、偏好和权限状态。 */
final case class LoginResponse(
  displayName: DisplayName,
  username: Username,
  avatarUrl: Option[UserAvatarUrl],
  email: EmailAddress,
  preferences: UserPreferences,
  siteManager: Boolean,
  problemManager: Boolean,
  contestManager: Boolean,
  message: String
)

/** 提供登录响应编解码和从账号/资料片段组装响应的入口。 */
object LoginResponse:
  given Encoder[LoginResponse] = deriveEncoder[LoginResponse]
  given Decoder[LoginResponse] = deriveDecoder[LoginResponse]

  /** 组合用户资料、账号邮箱和权限，归一化权限后生成登录响应。 */
  def fromParts(
    profile: UserProfileSettings,
    email: EmailAddress,
    siteManager: Boolean,
    problemManager: Boolean,
    contestManager: Boolean,
    message: String
  ): LoginResponse =
    val permissions = AuthPermissionFlags.normalize(siteManager, problemManager, contestManager)
    LoginResponse(
      displayName = profile.displayName,
      username = profile.username,
      avatarUrl = profile.avatarUrl,
      email = email,
      preferences = profile.preferences,
      siteManager = permissions.siteManager,
      problemManager = permissions.problemManager,
      contestManager = permissions.contestManager,
      message = message
    )
