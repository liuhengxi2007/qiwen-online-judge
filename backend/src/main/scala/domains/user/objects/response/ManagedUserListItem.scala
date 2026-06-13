package domains.user.objects.response

import domains.user.objects.*

import domains.auth.objects.EmailAddress
import domains.user.objects.{DisplayName, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 管理端用户列表项，合并用户资料、邮箱和权限状态。 */
final case class ManagedUserListItem(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  siteManager: Boolean,
  problemManager: Boolean,
  contestManager: Boolean
)

/** 提供管理端用户列表项 JSON 编解码。 */
object ManagedUserListItem:
  given Encoder[ManagedUserListItem] = deriveEncoder[ManagedUserListItem]
  given Decoder[ManagedUserListItem] = deriveDecoder[ManagedUserListItem]
