package domains.auth.objects.request

import domains.auth.objects.*
import domains.user.objects.{DisplayName, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 注册请求体，包含账号、展示名、邮箱和明文密码。 */
final case class RegisterRequest(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  password: PlaintextPassword
)

/** 提供注册请求 JSON 编解码；字段业务校验在 Register API 中完成。 */
object RegisterRequest:
  given Encoder[RegisterRequest] = deriveEncoder[RegisterRequest]
  given Decoder[RegisterRequest] = deriveDecoder[RegisterRequest]
