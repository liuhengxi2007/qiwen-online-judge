package domains.auth.http.mapper

import cats.effect.IO
import domains.auth.http.utils.AuthHttpSessionSupport
import domains.auth.model.SessionToken
import org.http4s.Request

object AuthHttpRequestMappers:

  def unit: Unit = ()

  def logoutInput(request: Request[IO]): Option[SessionToken] =
    AuthHttpSessionSupport.currentSessionToken(request)
