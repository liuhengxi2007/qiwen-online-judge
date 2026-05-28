package domains.auth.api

import cats.effect.IO
import domains.auth.objects.SessionToken
import domains.auth.utils.{AuthSessionCookies, SessionStore}
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.{Method, Request, Response, Status}
import shared.api.{ApiMessages, ApiPath, PathParams}
import io.circe.syntax.*
import shared.objects.response.SuccessResponse

import java.sql.Connection

final case class Logout(sessionStore: SessionStore) extends PublicResponseApi[Option[SessionToken]]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/auth/logout")

  override def decode(request: Request[IO], pathParams: PathParams): IO[Option[SessionToken]] =
    val _ = pathParams
    IO.pure(request.cookies.find(_.name == AuthSessionCookies.sessionCookieName).flatMap(cookie => SessionToken.parse(cookie.content).toOption))

  override def plan(connection: Connection, maybeToken: Option[SessionToken]): IO[Response[IO]] =
    val _ = connection
    val deleteSession = maybeToken match
      case Some(token) => sessionStore.deleteSession(token)
      case None => IO.unit

    deleteSession.as(
      Response[IO](status = Status.Ok)
        .withEntity(SuccessResponse.fromApiMessage(ApiMessages.loggedOut).asJson)
        .addCookie(AuthSessionCookies.clearedSessionCookie)
    )
