package domains.user.http.api



import domains.user.http.*
import cats.effect.IO
import domains.user.model.Username
import domains.user.http.UserHttpPlanDefinitions.{getUserSettings}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*

object GetUserSettings:

  def routes(context: UserHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "users" / targetUsername / "settings" =>
        context.handlers.execute(request, Username.canonical(targetUsername), getUserSettings)
    }

  private def parsePage(rawPage: Option[String]): Int =
    rawPage.flatMap(_.toIntOption).getOrElse(1)

  private def parsePageSize(rawPageSize: Option[String]): Int =
    rawPageSize.flatMap(_.toIntOption).filter(_ > 0).getOrElse(10)
