package domains.notification.http.api



import domains.notification.http.*
import domains.notification.http.mapper.NotificationHttpRequestMappers
import cats.effect.IO
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*
import org.http4s.HttpRoutes

object MarkNotificationRead:

  def routes(context: NotificationHttpRouteContext)(using Http4sDsl[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case request @ POST -> Root / "api" / "notifications" / rawNotificationId / "read" =>
        NotificationHttpRequestMappers.notificationId(rawNotificationId) match
          case Left(message) => shared.http.utils.HttpResponseSupport.validationErrorResponse(message)
          case Right(notificationId) =>
            context.handlers.execute(
              request,
              notificationId,
              context.plans.markNotificationRead
            )
    }
