package domains.usergroup.objects.request

import domains.usergroup.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 创建用户组请求，包含 slug、名称和描述。 */
final case class CreateUserGroupRequest(
  slug: UserGroupSlug,
  name: UserGroupName,
  description: UserGroupDescription
)

/** 提供创建用户组请求 JSON 编解码，字段校验在 mutation validation 中完成。 */
object CreateUserGroupRequest:
  given Encoder[CreateUserGroupRequest] = deriveEncoder[CreateUserGroupRequest]
  given Decoder[CreateUserGroupRequest] = deriveDecoder[CreateUserGroupRequest]
