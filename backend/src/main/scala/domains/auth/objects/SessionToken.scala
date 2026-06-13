package domains.auth.objects



/** 会话令牌值对象，用于 cookie、Redis 缓存和数据库会话表。 */
final case class SessionToken(value: String)

/** 提供会话令牌的基本输入解析。 */
object SessionToken:
  /** 解析非空会话令牌；不在此处校验随机性或是否存在。 */
  def parse(raw: String): Either[String, SessionToken] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Session token is required.")
    else Right(SessionToken(normalized))
