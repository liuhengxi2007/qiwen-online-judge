package domains.notification.http.api



import domains.notification.http.*
import cats.effect.IO
import domains.notification.model.NotificationId
import shared.http.utils.PageRequestQuerySupport
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*
import org.http4s.HttpRoutes

object ListNotifications:

  def routes(context: NotificationHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "notifications" =>
        context.handlers.execute(
          request,
          PageRequestQuerySupport.parsePageRequest(request.uri.query.params),
          context.plans.listNotifications
        )
    }
