package domains.auth.objects.response

import domains.user.objects.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 内部账号用户名解析响应，用于跨领域检查用户是否存在并回传规范用户名。 */
final case class ResolveAccountUsernameResponse(
  username: Option[Username]
)

/** 提供内部账号用户名解析响应的 JSON 编解码。 */
object ResolveAccountUsernameResponse:
  given Encoder[ResolveAccountUsernameResponse] = deriveEncoder[ResolveAccountUsernameResponse]
  given Decoder[ResolveAccountUsernameResponse] = deriveDecoder[ResolveAccountUsernameResponse]
