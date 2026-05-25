package domains.auth.http.mapper

import cats.effect.IO
import domains.auth.http.utils.AuthHttpSessionSupport
import domains.auth.model.SessionToken
import domains.auth.model.request.{LoginRequest, RegisterRequest}
import org.http4s.Request

object AuthHttpRequestMappers:

  def unit: Unit = ()

  def loginRequest(body: LoginRequest): LoginRequest =
    body

  def registerRequest(body: RegisterRequest): RegisterRequest =
    body

  def logoutInput(request: Request[IO]): Option[SessionToken] =
    AuthHttpSessionSupport.currentSessionToken(request)
