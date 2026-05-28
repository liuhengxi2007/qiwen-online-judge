package domains.auth.http

import cats.effect.IO
import domains.auth.utils.SessionStore
import domains.auth.objects.{AuthUser, SessionToken, SiteManagerUser}
import domains.auth.table.auth_user.AuthUserTable
import org.http4s.Request
import shared.http.{ApiMessages, HttpApiError}

import java.sql.Connection

final class SessionResolver(sessionStore: SessionStore):

  def currentSessionToken(request: Request[IO]): Option[SessionToken] =
    request.cookies.find(_.name == "qiwen_session").flatMap(cookie => SessionToken.parse(cookie.content).toOption)

  def resolveAuthUser(connection: Connection, request: Request[IO]): IO[AuthUser] =
    currentSessionToken(request) match
      case Some(token) =>
        sessionStore.lookupUsername(token).flatMap {
          case Some(username) =>
            AuthUserTable.findByUsername(connection, username).flatMap {
              case Some(user) => IO.pure(user)
              case None => HttpApiError.raise(HttpApiError.unauthorized(ApiMessages.authenticationRequired))
            }
          case None =>
            HttpApiError.raise(HttpApiError.unauthorized(ApiMessages.authenticationRequired))
        }
      case None =>
        HttpApiError.raise(HttpApiError.unauthorized(ApiMessages.authenticationRequired))

  def resolveSiteManager(connection: Connection, request: Request[IO]): IO[SiteManagerUser] =
    resolveAuthUser(connection, request).flatMap { user =>
      SiteManagerUser.from(user) match
        case Some(siteManagerUser) => IO.pure(siteManagerUser)
        case None => HttpApiError.raise(HttpApiError.forbidden(ApiMessages.siteManagerRequired))
    }
