package domains.user.objects

import io.circe.{Decoder, Encoder}


enum UserLocale:
  case En
  case ZhCn

object UserLocale:
  given Encoder[UserLocale] = Encoder.encodeString.contramap(encode)
  given Decoder[UserLocale] = Decoder.decodeString.emap(parse)

  def parse(value: String): Either[String, UserLocale] =
    value.trim match
      case "en" => Right(UserLocale.En)
      case "zh-CN" => Right(UserLocale.ZhCn)
      case _ => Left("User locale must be one of: en, zh-CN.")

  private def encode(value: UserLocale): String =
    value match
      case UserLocale.En => "en"
      case UserLocale.ZhCn => "zh-CN"
