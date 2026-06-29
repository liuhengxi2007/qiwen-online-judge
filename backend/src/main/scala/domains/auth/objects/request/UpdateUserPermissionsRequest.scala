package domains.auth.objects.request

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 站点管理员更新用户权限请求，字段会在服务端归一化。 */
final case class UpdateUserPermissionsRequest(
  siteManager: Boolean,
  problemManager: Boolean,
  contestManager: Boolean
)

/** 提供权限更新请求 JSON 编解码。 */
object UpdateUserPermissionsRequest:
  given Encoder[UpdateUserPermissionsRequest] = deriveEncoder[UpdateUserPermissionsRequest]
  given Decoder[UpdateUserPermissionsRequest] = deriveDecoder[UpdateUserPermissionsRequest]
