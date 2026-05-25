package domains.notification.http.api



import domains.notification.http.*
import domains.notification.http.mapper.NotificationHttpRequestMappers
import cats.effect.IO
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*
import org.http4s.HttpRoutes

object ListNotifications:

  def routes(context: NotificationHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ GET -> Root / "api" / "notifications" =>
        context.handlers.execute(
          request,
          NotificationHttpRequestMappers.listNotificationsRequest(request.uri.query.params),
          context.plans.listNotifications
        )
    }
