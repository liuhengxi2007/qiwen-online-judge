package domains.user.http.api



import domains.user.http.*
import cats.effect.IO
import domains.user.model.Username
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object UpdateUserPreferences:

  def routes(context: UserHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "users" / targetUsername / "settings" / "preferences" =>
        context.handlers.executeUserSettingsPreferencesUpdate(request, Username.canonical(targetUsername))
    }

  private def parsePage(rawPage: Option[String]): Int =
    rawPage.flatMap(_.toIntOption).getOrElse(1)

  private def parsePageSize(rawPageSize: Option[String]): Int =
    rawPageSize.flatMap(_.toIntOption).filter(_ > 0).getOrElse(10)
