package domains.auth.objects

import io.circe.{Decoder, Encoder}

/** 明文密码输入值对象，仅用于请求解码和哈希校验边界。 */
final case class PlaintextPassword(value: String)

/** 提供明文密码的 JSON 编解码；不在此处做强度校验。 */
object PlaintextPassword:
  given Encoder[PlaintextPassword] = Encoder.encodeString.contramap(_.value)
  given Decoder[PlaintextPassword] = Decoder.decodeString.map(PlaintextPassword(_))
