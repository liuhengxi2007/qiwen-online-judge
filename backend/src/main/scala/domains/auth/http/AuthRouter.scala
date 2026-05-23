package domains.auth.http



import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.auth.http.api.GetSession
import domains.auth.http.api.Logout
import domains.auth.http.api.Login
import domains.auth.http.api.Register
import domains.auth.application.SessionStore
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object AuthRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val context = AuthHttpRouteContext(
      databaseSession = databaseSession,
      sessionStore = sessionStore,
      handlers = new AuthHttpHandlers(databaseSession, sessionStore)
    )

    GetSession.routes(context) <+>
      Logout.routes(context) <+>
      Login.routes(context) <+>
      Register.routes(context)
