package domains.usergroup.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 内部用户组 slug 解析响应，用于跨领域检查 slug 是否已被用户组占用。 */
final case class ResolveUserGroupSlugResponse(
  exists: Boolean
)

/** 提供用户组 slug 解析响应 JSON 编解码。 */
object ResolveUserGroupSlugResponse:
  given Encoder[ResolveUserGroupSlugResponse] = deriveEncoder[ResolveUserGroupSlugResponse]
  given Decoder[ResolveUserGroupSlugResponse] = deriveDecoder[ResolveUserGroupSlugResponse]
