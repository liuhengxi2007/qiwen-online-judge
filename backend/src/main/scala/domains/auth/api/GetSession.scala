package domains.auth.api

import cats.effect.IO
import domains.auth.objects.internal.AuthenticatedUser
import domains.auth.objects.response.SessionResponse
import domains.auth.table.auth_account.AuthAccountTable
import domains.user.api.FindUserProfileSettings
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

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

  override def plan(connection: Connection, actor: AuthenticatedUser, input: Unit): IO[SessionResponse] =
    val _ = input
    for
      account <- AuthAccountTable.findAccountByUsername(connection, actor.username).flatMap {
        case Some(account) => IO.pure(account)
        case None => HttpApiError.raise(HttpApiError.unauthorized(ApiMessages.authenticationRequired))
      }
      profile <- FindUserProfileSettings.plan(connection, actor.username).flatMap {
        case Some(profile) => IO.pure(profile)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      }
    yield SessionResponse.fromParts(profile, account.email, account.siteManager, account.problemManager, account.contestManager)
