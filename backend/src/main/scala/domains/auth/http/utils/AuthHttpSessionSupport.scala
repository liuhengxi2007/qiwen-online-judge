package domains.auth.http.utils



import domains.auth.http.mapper.AuthHttpResponseMappers
import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.model.{AuthUser, SessionToken, SiteManagerUser}
import domains.auth.table.auth_user.AuthUserTable
import org.http4s.{Request, Response}

object AuthHttpSessionSupport:

  def currentSessionToken(request: Request[IO]): Option[SessionToken] =
    request.cookies.find(_.name == "qiwen_session").flatMap(cookie => SessionToken.parse(cookie.content).toOption)

  def authenticatedUser(
    databaseSession: DatabaseSession,
    sessionStore: SessionStore,
    request: Request[IO]
  ): IO[Option[AuthUser]] =
    currentSessionToken(request) match
      case Some(token) =>
        sessionStore.lookupUsername(token).flatMap {
          case Some(username) =>
            databaseSession.withTransactionConnection(connection =>
              AuthUserTable.findByUsername(connection, username)
            )
          case None =>
            IO.pure(None)
        }
      case None =>
        IO.pure(None)

  def withAuthenticatedUser(
    databaseSession: DatabaseSession,
    sessionStore: SessionStore,
    request: Request[IO]
  )(handle: AuthUser => IO[Response[IO]]): IO[Response[IO]] =
    authenticatedUser(databaseSession, sessionStore, request).flatMap {
      case Some(user) => handle(user)
      case None => AuthHttpResponseMappers.unauthorizedResponse
    }

  def withSiteManager(
    databaseSession: DatabaseSession,
    sessionStore: SessionStore,
    request: Request[IO]
  )(handle: SiteManagerUser => IO[Response[IO]]): IO[Response[IO]] =
    withAuthenticatedUser(databaseSession, sessionStore, request) { user =>
      SiteManagerUser.from(user) match
        case Some(siteManagerUser) => handle(siteManagerUser)
        case None => AuthHttpResponseMappers.forbiddenResponse
    }
