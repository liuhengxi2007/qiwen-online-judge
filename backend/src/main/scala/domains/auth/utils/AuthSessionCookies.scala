package domains.auth.utils

import domains.auth.objects.SessionToken
import org.http4s.{ResponseCookie, SameSite}

/** 认证会话 cookie 构造工具，集中设置 cookie 名称和安全属性。 */
object AuthSessionCookies:
  val sessionCookieName: String = "qiwen_session"

  /** 构造登录后的 httpOnly Lax 会话 cookie。 */
  def sessionCookie(token: SessionToken): ResponseCookie =
    val config = AuthSessionCookieConfig.default
    ResponseCookie(
      name = sessionCookieName,
      content = token.value,
      path = Some("/"),
      secure = config.secure,
      httpOnly = true,
      sameSite = Some(SameSite.Lax),
      maxAge = Some(config.maxAgeSeconds)
    )

  def clearedSessionCookie: ResponseCookie =
    val config = AuthSessionCookieConfig.default
    ResponseCookie(
      name = sessionCookieName,
      content = "",
      path = Some("/"),
      secure = config.secure,
      httpOnly = true,
      sameSite = Some(SameSite.Lax),
      maxAge = Some(0L)
    )

private final case class AuthSessionCookieConfig(secure: Boolean, maxAgeSeconds: Long)

private object AuthSessionCookieConfig:
  def default: AuthSessionCookieConfig =
    AuthSessionCookieConfig(
      secure = configuredSecure,
      maxAgeSeconds = SessionConfig.default.ttl.toSeconds
    )

  private def configuredSecure: Boolean =
    sys.env.get("AUTH_SESSION_COOKIE_SECURE").map(_.trim.toLowerCase) match
      case Some("true" | "1" | "yes" | "on") => true
      case Some("false" | "0" | "no" | "off") => false
      case Some(_) => throw IllegalArgumentException("AUTH_SESSION_COOKIE_SECURE must be a boolean value.")
      case None =>
        !sys.env
          .get("APP_ENV")
          .map(_.trim.toLowerCase)
          .exists(env => Set("dev", "development", "local", "test").contains(env))
