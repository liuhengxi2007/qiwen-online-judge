package domains.user.model



enum UserLocale:
  case En
  case ZhCn

object UserLocale:
  def parse(value: String): Either[String, UserLocale] =
    value.trim match
      case "en" => Right(UserLocale.En)
      case "zh-CN" => Right(UserLocale.ZhCn)
      case _ => Left("User locale must be one of: en, zh-CN.")

  def fromDatabase(value: String): Option[UserLocale] =
    value match
      case "en" => Some(UserLocale.En)
      case "zh-CN" => Some(UserLocale.ZhCn)
      case _ => None

  def toDatabase(value: UserLocale): String =
    value match
      case UserLocale.En => "en"
      case UserLocale.ZhCn => "zh-CN"
