package domains.user.objects

import io.circe.{Decoder, Encoder}


/** 用户界面语言偏好。 */
enum UserLocale:
  case En
  case ZhCn

/** 提供语言偏好的 JSON 编解码和输入解析。 */
object UserLocale:
  given Encoder[UserLocale] = Encoder.encodeString.contramap(encode)
  given Decoder[UserLocale] = Decoder.decodeString.emap(parse)

  /** 解析传输层语言代码，非法值返回业务校验错误。 */
  def parse(value: String): Either[String, UserLocale] =
    value.trim match
      case "en" => Right(UserLocale.En)
      case "zh-CN" => Right(UserLocale.ZhCn)
      case _ => Left("User locale must be one of: en, zh-CN.")

  private def encode(value: UserLocale): String =
    value match
      case UserLocale.En => "en"
      case UserLocale.ZhCn => "zh-CN"
