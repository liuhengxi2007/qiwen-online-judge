package domains.user.objects

import io.circe.{Decoder, Encoder}

/** 用户展示名值对象，用于资料页和身份展示。 */
final case class DisplayName(value: String)

/** 提供展示名 JSON 编解码；长度和必填校验在业务入口完成。 */
object DisplayName:
  given Encoder[DisplayName] = Encoder.encodeString.contramap(_.value)
  given Decoder[DisplayName] = Decoder.decodeString.map(DisplayName(_))
