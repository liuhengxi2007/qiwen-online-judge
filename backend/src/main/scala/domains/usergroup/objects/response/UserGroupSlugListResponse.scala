package domains.usergroup.objects.response

import domains.usergroup.objects.UserGroupSlug
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 用户所属用户组 slug 列表响应，供内部访问控制查询使用。 */
final case class UserGroupSlugListResponse(
  slugs: List[UserGroupSlug]
)

/** 提供用户组 slug 列表响应 JSON 编解码。 */
object UserGroupSlugListResponse:
  given Encoder[UserGroupSlugListResponse] = deriveEncoder[UserGroupSlugListResponse]
  given Decoder[UserGroupSlugListResponse] = deriveDecoder[UserGroupSlugListResponse]
