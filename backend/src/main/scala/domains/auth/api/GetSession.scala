package domains.auth.api

import cats.effect.IO
import domains.auth.objects.AuthUser
import domains.auth.objects.response.SessionResponse
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, PathParams}

import java.sql.Connection

object GetSession extends AuthenticatedApi[Unit, SessionResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/auth/session")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SessionResponse] = summon[Encoder[SessionResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = request
    val _ = pathParams
    IO.unit

  override def plan(connection: Connection, actor: AuthUser, input: Unit): IO[SessionResponse] =
    val _ = connection
    val _ = input
    IO.pure(SessionResponse.fromAuthUser(actor))
