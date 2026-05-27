package domains.auth.http.mapper

import cats.effect.IO
import domains.auth.http.utils.AuthHttpSessionSupport
import domains.auth.objects.SessionToken
import domains.auth.objects.request.UpdateUserPermissionsRequest
import domains.user.objects.Username
import org.http4s.Request

object AuthHttpRequestMappers:

  def unit: Unit = ()

  def logoutInput(request: Request[IO]): Option[SessionToken] =
    AuthHttpSessionSupport.currentSessionToken(request)

  def username(rawUsername: String): Username =
    Username.canonical(rawUsername)

  def updateUserPermissionsInput(
    rawUsername: String,
    body: UpdateUserPermissionsRequest
  ): (Username, UpdateUserPermissionsRequest) =
    (Username.canonical(rawUsername), body)
