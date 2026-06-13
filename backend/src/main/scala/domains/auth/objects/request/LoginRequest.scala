package domains.auth.objects.request

import domains.auth.objects.*
import domains.user.objects.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 登录请求体，包含用户名和明文密码输入。 */
final case class LoginRequest(username: Username, password: PlaintextPassword)

/** 提供登录请求 JSON 编解码。 */
object LoginRequest:
  given Encoder[LoginRequest] = deriveEncoder[LoginRequest]
  given Decoder[LoginRequest] = deriveDecoder[LoginRequest]
