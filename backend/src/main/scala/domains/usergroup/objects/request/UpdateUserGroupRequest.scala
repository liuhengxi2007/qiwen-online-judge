package domains.usergroup.objects.request

import domains.usergroup.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 更新用户组基础资料请求，当前只允许修改名称和描述。 */
final case class UpdateUserGroupRequest(
  name: UserGroupName,
  description: UserGroupDescription
)

/** 提供用户组更新请求 JSON 编解码。 */
object UpdateUserGroupRequest:
  given Encoder[UpdateUserGroupRequest] = deriveEncoder[UpdateUserGroupRequest]
  given Decoder[UpdateUserGroupRequest] = deriveDecoder[UpdateUserGroupRequest]
