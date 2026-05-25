package domains.user.http.api



import domains.user.http.*
import domains.user.http.mapper.UserHttpRequestMappers
import cats.effect.IO
import domains.user.http.UserHttpPlanDefinitions.{getUserProfile}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object GetUserProfile:

  def routes(handlers: UserHttpHandlers)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "users" / targetUsername / "profile" =>
        handlers.execute(request, UserHttpRequestMappers.username(targetUsername), getUserProfile)
    }
