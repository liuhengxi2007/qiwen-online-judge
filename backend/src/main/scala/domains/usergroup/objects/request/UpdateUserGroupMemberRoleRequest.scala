package domains.usergroup.objects.request

import domains.usergroup.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 更新用户组成员角色请求，owner 变更会触发所有权转移逻辑。 */
final case class UpdateUserGroupMemberRoleRequest(
  role: UserGroupRole
)

/** 提供成员角色更新请求 JSON 编解码。 */
object UpdateUserGroupMemberRoleRequest:
  given Encoder[UpdateUserGroupMemberRoleRequest] = deriveEncoder[UpdateUserGroupMemberRoleRequest]
  given Decoder[UpdateUserGroupMemberRoleRequest] = deriveDecoder[UpdateUserGroupMemberRoleRequest]
