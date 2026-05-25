package domains.auth.http



import cats.effect.IO
import cats.syntax.semigroupk.*
import database.DatabaseSession
import domains.auth.http.api.GetSession
import domains.auth.http.api.Logout
import domains.auth.http.api.Login
import domains.auth.http.api.Register
import domains.auth.http.api.UpdateAccount
import domains.auth.http.api.UpdateAccountPermissions
import domains.auth.http.api.DeleteAccount
import domains.auth.application.SessionStore
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object AuthRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthHttpHandlers(databaseSession, sessionStore)

    GetSession.routes(handlers) <+>
      Logout.routes(handlers) <+>
      Login.routes(handlers) <+>
      Register.routes(handlers) <+>
      UpdateAccount.routes(handlers) <+>
      UpdateAccountPermissions.routes(handlers) <+>
      DeleteAccount.routes(handlers)
