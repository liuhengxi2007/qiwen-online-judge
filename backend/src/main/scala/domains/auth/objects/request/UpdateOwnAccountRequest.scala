package domains.auth.objects.request

import domains.auth.objects.{EmailAddress, PlaintextPassword}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 用户更新自己账号请求，必须携带当前密码以确认敏感操作。 */
final case class UpdateOwnAccountRequest(
  email: EmailAddress,
  currentPassword: PlaintextPassword,
  newPassword: Option[PlaintextPassword]
)

/** 提供自助账号更新请求 JSON 编解码。 */
object UpdateOwnAccountRequest:
  given Encoder[UpdateOwnAccountRequest] = deriveEncoder[UpdateOwnAccountRequest]
  given Decoder[UpdateOwnAccountRequest] = deriveDecoder[UpdateOwnAccountRequest]
