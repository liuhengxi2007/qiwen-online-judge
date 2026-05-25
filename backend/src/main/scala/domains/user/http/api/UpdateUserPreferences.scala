package domains.user.http.api



import domains.user.http.*
import domains.user.http.mapper.UserHttpRequestMappers
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UpdateUserPreferences:

  def routes(handlers: UserHttpHandlers)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "users" / targetUsername / "settings" / "preferences" =>
        handlers.executeUserSettingsPreferencesUpdate(request, UserHttpRequestMappers.username(targetUsername))
    }
