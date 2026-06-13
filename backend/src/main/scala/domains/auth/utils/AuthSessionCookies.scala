package domains.auth.utils

import domains.auth.objects.SessionToken
import org.http4s.{ResponseCookie, SameSite}

/** 认证会话 cookie 构造工具，集中设置 cookie 名称和安全属性。 */
object AuthSessionCookies:
  val sessionCookieName: String = "qiwen_session"

  /** 构造登录后的 httpOnly Lax 会话 cookie。 */
  def sessionCookie(token: SessionToken): ResponseCookie =
    /** FIXME-CN: 会话 cookie 未设置 secure/maxAge，生产 HTTPS 或显式浏览器生命周期策略需要由配置补齐。 */
    ResponseCookie(
      name = sessionCookieName,
      content = token.value,
      path = Some("/"),
      httpOnly = true,
      sameSite = Some(SameSite.Lax)
    )

  val clearedSessionCookie: ResponseCookie =
    ResponseCookie(
      name = sessionCookieName,
      content = "",
      path = Some("/"),
      httpOnly = true,
      sameSite = Some(SameSite.Lax),
      maxAge = Some(0L)
    )
