package domains.auth.objects.response

import domains.auth.objects.{AuthPermissionFlags, EmailAddress}
import domains.user.objects.UserProfileSettings

import domains.user.objects.{DisplayName, UserAvatarUrl, UserPreferences, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 注册成功响应，形状与登录响应保持一致以便前端立即建立会话状态。 */
final case class RegisterResponse(
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

/** 提供注册响应编解码和从账号/资料片段组装响应的入口。 */
object RegisterResponse:
  given Encoder[RegisterResponse] = deriveEncoder[RegisterResponse]
  given Decoder[RegisterResponse] = deriveDecoder[RegisterResponse]

  /** 组合新建用户资料、账号邮箱和权限，归一化权限后生成注册响应。 */
  def fromParts(
    profile: UserProfileSettings,
    email: EmailAddress,
    siteManager: Boolean,
    problemManager: Boolean,
    contestManager: Boolean,
    message: String
  ): RegisterResponse =
    val permissions = AuthPermissionFlags.normalize(siteManager, problemManager, contestManager)
    RegisterResponse(
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
