package domains.user.http.api



import domains.user.http.*
import domains.user.http.mapper.UserHttpRequestMappers
import cats.effect.IO
import domains.user.http.UserHttpPlanDefinitions.{listUsers}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object ListUsers:

  def routes(handlers: UserHttpHandlers)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "users" =>
        handlers.execute(
          request,
          UserHttpRequestMappers.listUsersRequest(request.uri.query.params),
          listUsers
        )
    }
