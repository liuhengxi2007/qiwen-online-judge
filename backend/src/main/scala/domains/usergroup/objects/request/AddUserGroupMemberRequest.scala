package domains.usergroup.objects.request

import domains.user.objects.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 添加用户组成员请求，包含目标用户名和初始成员角色。 */
final case class AddUserGroupMemberRequest(
  username: Username,
  role: NewUserGroupMemberRole
)

/** 提供添加成员请求 JSON 编解码。 */
object AddUserGroupMemberRequest:
  given Encoder[AddUserGroupMemberRequest] = deriveEncoder[AddUserGroupMemberRequest]
  given Decoder[AddUserGroupMemberRequest] = deriveDecoder[AddUserGroupMemberRequest]
