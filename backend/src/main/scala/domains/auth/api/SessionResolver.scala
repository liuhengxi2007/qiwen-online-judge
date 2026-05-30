package domains.auth.api

import cats.effect.IO
import domains.auth.objects.{SessionToken, SiteManagerUser}
import domains.auth.objects.internal.AuthenticatedUser
import domains.auth.table.auth_account.AuthAccountTable
import domains.auth.utils.{AuthSessionCookies, SessionStore}
import org.http4s.Request
import shared.api.{ApiMessages, HttpApiError}

import java.sql.Connection

final class SessionResolver(sessionStore: SessionStore):

  def currentSessionToken(request: Request[IO]): Option[SessionToken] =
    request.cookies.find(_.name == AuthSessionCookies.sessionCookieName).flatMap(cookie => SessionToken.parse(cookie.content).toOption)

  def resolveAuthenticatedUser(connection: Connection, request: Request[IO]): IO[AuthenticatedUser] =
    currentSessionToken(request) match
      case Some(token) =>
        sessionStore.lookupUsername(token).flatMap {
          case Some(username) =>
            AuthAccountTable.findAuthenticatedUserByUsername(connection, username).flatMap {
              case Some(user) => IO.pure(user)
              case None => HttpApiError.raise(HttpApiError.unauthorized(ApiMessages.authenticationRequired))
            }
          case None =>
            HttpApiError.raise(HttpApiError.unauthorized(ApiMessages.authenticationRequired))
        }
      case None =>
        HttpApiError.raise(HttpApiError.unauthorized(ApiMessages.authenticationRequired))

  def resolveSiteManager(connection: Connection, request: Request[IO]): IO[SiteManagerUser] =
    resolveAuthenticatedUser(connection, request).flatMap { user =>
      SiteManagerUser.from(user) match
        case Some(siteManagerUser) => IO.pure(siteManagerUser)
        case None => HttpApiError.raise(HttpApiError.forbidden(ApiMessages.siteManagerRequired))
    }
