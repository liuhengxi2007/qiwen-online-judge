package domains.auth.http.api



import domains.auth.http.utils.AuthHttpSessionSupport
import domains.auth.http.*
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object Logout:

  def routes(context: AuthHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "auth" / "logout" =>
        context.handlers.execute(request, AuthHttpSessionSupport.currentSessionToken(request), AuthHttpPlanDefinitions.logout)
    }
