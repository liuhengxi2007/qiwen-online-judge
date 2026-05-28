package domains.auth.api

import cats.effect.IO
import domains.auth.objects.response.LoginResponse
import domains.auth.utils.{AuthSessionCookies, PasswordHasher, SessionStore}
import domains.auth.objects.request.LoginRequest
import domains.auth.table.auth_user.AuthUserTable
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Response, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import io.circe.syntax.*

import java.sql.Connection

final case class Login(sessionStore: SessionStore) extends PublicResponseApi[LoginRequest]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/auth/login")

  override def decode(request: Request[IO], pathParams: PathParams): IO[LoginRequest] =
    val _ = pathParams
    request.as[LoginRequest]

  override def plan(connection: Connection, request: LoginRequest): IO[Response[IO]] =
    AuthUserTable.findByUsername(connection, request.username).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.unauthorized(ApiMessages.invalidCredentials))
      case Some(user) =>
        PasswordHasher.verifyPassword(request.password, user.passwordHash).flatMap {
          case false =>
            HttpApiError.raise(HttpApiError.unauthorized(ApiMessages.invalidCredentials))
          case true =>
            sessionStore
              .createSessionInConnection(connection, user.username)
              .map(sessionToken =>
                Response[IO](status = Status.Ok)
                  .withEntity(LoginResponse.fromAuthUser(user, "Login successful").asJson)
                  .addCookie(AuthSessionCookies.sessionCookie(sessionToken))
              )
        }
    }
