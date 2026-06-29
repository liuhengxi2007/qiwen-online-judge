package domains.user.objects.response

import domains.auth.objects.{AuthPermissionFlags, EmailAddress}
import domains.user.objects.UserProfileSettings
import domains.user.objects.{DisplayName, UserPreferences, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 用户设置响应，供个人设置页和管理端用户设置页使用。 */
final case class UserSettingsResponse(
  displayName: DisplayName,
  username: Username,
  avatarUrl: Option[domains.user.objects.UserAvatarUrl],
  email: EmailAddress,
  preferences: UserPreferences,
  siteManager: Boolean,
  problemManager: Boolean,
  contestManager: Boolean
)

/** 提供用户设置响应编解码和从资料/账号片段组装响应的入口。 */
object UserSettingsResponse:
  given Encoder[UserSettingsResponse] = deriveEncoder[UserSettingsResponse]
  given Decoder[UserSettingsResponse] = deriveDecoder[UserSettingsResponse]

  /** 合并用户资料、账号邮箱和权限布尔值，并在输出前归一化权限。 */
  def fromParts(
    profile: UserProfileSettings,
    email: EmailAddress,
    siteManager: Boolean,
    problemManager: Boolean,
    contestManager: Boolean
  ): UserSettingsResponse =
    val permissions = AuthPermissionFlags.normalize(siteManager, problemManager, contestManager)
    UserSettingsResponse(
      displayName = profile.displayName,
      username = profile.username,
      avatarUrl = profile.avatarUrl,
      email = email,
      preferences = profile.preferences,
      siteManager = permissions.siteManager,
      problemManager = permissions.problemManager,
      contestManager = permissions.contestManager
    )
