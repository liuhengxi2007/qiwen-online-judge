package domains.auth.api

import cats.effect.IO
import domains.auth.objects.response.LoginResponse
import domains.auth.utils.{AuthSessionCookies, PasswordHasher, SessionStore}
import domains.auth.objects.request.LoginRequest
import domains.auth.table.auth_account.AuthAccountTable
import domains.user.api.UserProfileRecords
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
    AuthAccountTable.findAccountByUsername(connection, request.username).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.unauthorized(ApiMessages.invalidCredentials))
      case Some(account) =>
        PasswordHasher.verifyPassword(request.password, account.passwordHash).flatMap {
          case false =>
            HttpApiError.raise(HttpApiError.unauthorized(ApiMessages.invalidCredentials))
          case true =>
            for
              profile <- findProfile(connection, account.username)
              sessionToken <- sessionStore.createSessionInConnection(connection, account.username)
            yield
                Response[IO](status = Status.Ok)
                  .withEntity(
                    LoginResponse
                      .fromParts(profile, account.email, account.siteManager, account.problemManager, "Login successful")
                      .asJson
                  )
                  .addCookie(AuthSessionCookies.sessionCookie(sessionToken))
        }
    }

  private def findProfile(connection: Connection, username: domains.user.objects.Username) =
    UserProfileRecords.findSettings(connection, username).flatMap {
      case Some(profile) => IO.pure(profile)
      case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
    }
