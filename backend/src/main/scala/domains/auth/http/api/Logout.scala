package domains.auth.http.api

import cats.effect.IO
import domains.auth.utils.SessionStore
import domains.auth.http.{AuthApiSupport, PublicResponseApi}
import domains.auth.objects.SessionToken
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.{Method, Request, Response, Status}
import shared.http.codec.SharedHttpCodecs.given
import shared.http.{ApiPath, PathParams}
import io.circe.syntax.*

import java.sql.Connection

final case class Logout(sessionStore: SessionStore) extends PublicResponseApi[Option[SessionToken]]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/auth/logout")

  override def decode(request: Request[IO], pathParams: PathParams): IO[Option[SessionToken]] =
    val _ = pathParams
    IO.pure(request.cookies.find(_.name == "qiwen_session").flatMap(cookie => SessionToken.parse(cookie.content).toOption))

  override def plan(connection: Connection, maybeToken: Option[SessionToken]): IO[Response[IO]] =
    val _ = connection
    val deleteSession = maybeToken match
      case Some(token) => sessionStore.deleteSession(token)
      case None => IO.unit

    deleteSession.as(
      Response[IO](status = Status.Ok)
        .withEntity(AuthApiSupport.loggedOutSuccess.asJson)
        .addCookie(AuthApiSupport.clearedSessionCookie)
    )
