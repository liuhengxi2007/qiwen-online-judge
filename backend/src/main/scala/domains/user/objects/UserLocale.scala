package domains.user.objects



enum UserLocale:
  case En
  case ZhCn

object UserLocale:
  def parse(value: String): Either[String, UserLocale] =
    value.trim match
      case "en" => Right(UserLocale.En)
      case "zh-CN" => Right(UserLocale.ZhCn)
      case _ => Left("User locale must be one of: en, zh-CN.")
