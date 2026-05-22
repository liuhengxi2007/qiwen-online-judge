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

object AuthRouter:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    GetSession.routes(databaseSession, sessionStore) <+>
      Logout.routes(databaseSession, sessionStore) <+>
      Login.routes(databaseSession, sessionStore) <+>
      Register.routes(databaseSession, sessionStore)
