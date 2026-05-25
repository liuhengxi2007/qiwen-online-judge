package domains.auth.http.api



import domains.auth.http.*
import domains.auth.http.mapper.AuthHttpRequestMappers
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object GetSession:

  def routes(handlers: AuthHttpHandlers)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "auth" / "session" =>
        handlers.execute(request, AuthHttpRequestMappers.unit, AuthHttpPlanDefinitions.session)
    }
