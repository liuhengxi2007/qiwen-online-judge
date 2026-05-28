package domains.auth.utils

import domains.auth.objects.SessionToken
import org.http4s.{ResponseCookie, SameSite}

object AuthSessionCookies:
  val sessionCookieName: String = "qiwen_session"

  def sessionCookie(token: SessionToken): ResponseCookie =
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
