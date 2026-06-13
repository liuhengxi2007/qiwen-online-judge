package domains.auth.objects

import io.circe.{Decoder, Encoder}

/** 登录账号邮箱地址值对象，避免在账号接口中直接传递裸字符串。 */
final case class EmailAddress(value: String)

/** 提供邮箱 JSON 编解码和业务格式校验。 */
object EmailAddress:
  given Encoder[EmailAddress] = Encoder.encodeString.contramap(_.value)
  given Decoder[EmailAddress] = Decoder.decodeString.map(EmailAddress(_))

  /** 校验邮箱必填、长度和基本格式；合法返回 None，非法返回可展示错误文本。 */
  def validationMessage(email: EmailAddress): Option[String] =
    val normalized = email.value.trim
    val emailPattern = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$".r

    if normalized.isEmpty then Some("Email is required.")
    else if normalized.length > 255 then Some("Email must be at most 255 characters.")
    else if emailPattern.matches(normalized) then None
    else Some("Please enter a valid email address.")
