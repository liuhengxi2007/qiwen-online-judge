package shared.objects.access

import java.util.Locale

/** 访问控制系统使用的规范化用户名，避免在共享授权对象中直接传递裸字符串。 */
final case class AccessUsername(value: String)

/** 提供授权用户名的规范化与格式校验，输入来自 API 或数据库映射边界。 */
object AccessUsername:
  private val usernamePattern = "^[a-z0-9_-]+$".r

  /** 将用户名去空白并转为小写，作为授权比较的规范形式。 */
  def normalize(raw: String): String =
    raw.trim.toLowerCase(Locale.ROOT)

  /** 构造规范化用户名；调用方需确保原始值已经可信或随后会校验。 */
  def canonical(raw: String): AccessUsername =
    AccessUsername(normalize(raw))

  /** 校验并解析外部输入用户名，限制长度和允许字符，无副作用。 */
  def parse(raw: String): Either[String, AccessUsername] =
    val normalized = normalize(raw)

    if normalized.length < 3 || normalized.length > 32 then
      Left("Username must be between 3 and 32 characters.")
    else if !usernamePattern.matches(normalized) then
      Left("Username may contain only lowercase letters, numbers, underscores, and hyphens.")
    else
      Right(AccessUsername(normalized))
