package domains.auth.http.api



import domains.auth.http.*
import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object GetSession:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthHttpHandlers(databaseSession, sessionStore)
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "auth" / "session" =>
        handlers.execute(request, (), AuthHttpPlanDefinitions.session)
    }
