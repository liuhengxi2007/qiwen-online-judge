package domains.user.objects

import io.circe.{Decoder, Encoder}

import java.util.Locale

/** 规范化用户名值对象，所有账号和资料查询都应优先使用该类型。 */
final case class Username(value: String)

/** 提供用户名 JSON 编解码、规范化和格式校验。 */
object Username:
  given Encoder[Username] = Encoder.encodeString.contramap(_.value)
  given Decoder[Username] = Decoder.decodeString.emap(parse)

  private val usernamePattern = "^[a-z0-9_-]+$".r

  /** 将输入用户名去空白并转为小写，作为系统内比较和存储的规范形式。 */
  def normalize(raw: String): String =
    raw.trim.toLowerCase(Locale.ROOT)

  /** 构造规范用户名；调用方需确保该值来源可信或已在上游校验。 */
  def canonical(raw: String): Username =
    new Username(normalize(raw))

  /** 校验用户名长度和字符集，返回规范化用户名或业务错误。 */
  def parse(raw: String): Either[String, Username] =
    val normalized = normalize(raw)

    if normalized.length < 3 || normalized.length > 32 then
      Left("Username must be between 3 and 32 characters.")
    else if !usernamePattern.matches(normalized) then
      Left("Username may contain only lowercase letters, numbers, underscores, and hyphens.")
    else
      Right(Username(normalized))
