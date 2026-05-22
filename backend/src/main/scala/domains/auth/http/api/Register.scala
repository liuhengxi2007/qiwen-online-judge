package domains.auth.http.api



import domains.auth.http.*
import cats.effect.IO
import database.DatabaseSession
import domains.auth.application.SessionStore
import domains.auth.application.input.RegisterRequest
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object Register:

  def routes(databaseSession: DatabaseSession, sessionStore: SessionStore): HttpRoutes[IO] =
    given Http4sDsl[IO] = new Http4sDsl[IO] {}
    val handlers = new AuthHttpHandlers(databaseSession, sessionStore)
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "auth" / "register" =>
        handlers.executeDecoded[RegisterRequest, RegisterRequest, AuthHttpPlans.RegisterOutput](
          request,
          AuthHttpPlanDefinitions.register
        )(identity)
    }
