package domains.auth.objects.response

import domains.auth.objects.{AuthPermissionFlags, EmailAddress}
import domains.auth.objects.internal.AuthAccount
import domains.user.objects.{DisplayName, Username}
import domains.user.objects.UserProfileSettings
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 管理端账号列表项，合并账号邮箱、展示名和归一化权限。 */
final case class AuthAccountListItem(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  siteManager: Boolean,
  problemManager: Boolean,
  contestManager: Boolean
)

/** 提供管理端账号列表项编解码和内部模型转换。 */
object AuthAccountListItem:
  given Encoder[AuthAccountListItem] = deriveEncoder[AuthAccountListItem]
  given Decoder[AuthAccountListItem] = deriveDecoder[AuthAccountListItem]

  /** 从账号记录和用户资料组合前端列表项，不访问数据库。 */
  def fromParts(account: AuthAccount, profile: UserProfileSettings): AuthAccountListItem =
    val permissions = AuthPermissionFlags.normalize(account.siteManager, account.problemManager, account.contestManager)
    AuthAccountListItem(
      username = account.username,
      displayName = profile.displayName,
      email = account.email,
      siteManager = permissions.siteManager,
      problemManager = permissions.problemManager,
      contestManager = permissions.contestManager
    )
