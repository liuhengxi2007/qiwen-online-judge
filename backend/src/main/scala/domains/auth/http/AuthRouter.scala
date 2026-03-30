package domains.auth.http

import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.model.Username
import org.http4s.HttpRoutes
import org.http4s.dsl.io.*

object AuthRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = Http4sDsl[IO]
    val sessionSupport = new AuthHttpSessionSupport(databaseSession, sessionStore)
    val handlers = new AuthHttpHandlers(databaseSession, sessionStore, sessionSupport)

    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "auth" / "session" =>
        handlers.session(request)

      case request @ POST -> Root / "api" / "auth" / "logout" =>
        handlers.logout(request)

      case request @ GET -> Root / "api" / "auth" / "users" =>
        handlers.listUsers(request)

      case request @ GET -> Root / "api" / "auth" / "users" / targetUsername / "settings" =>
        handlers.getUserSettings(request, Username(targetUsername))

      case request @ POST -> Root / "api" / "auth" / "users" / targetUsername / "permissions" =>
        handlers.updateUserPermissions(request, Username(targetUsername))

      case request @ POST -> Root / "api" / "auth" / "users" / targetUsername / "settings" =>
        handlers.updateUserSettings(request, Username(targetUsername))

      case request @ POST -> Root / "api" / "auth" / "login" =>
        handlers.login(request)

      case request @ POST -> Root / "api" / "auth" / "register" =>
        handlers.register(request)
    }
