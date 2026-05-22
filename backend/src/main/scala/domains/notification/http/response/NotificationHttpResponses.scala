package domains.notification.http.response



import cats.effect.IO
import domains.notification.application.NotificationCommands
import domains.notification.http.codec.NotificationHttpCodecs.given
import shared.http.ApiMessages
import shared.http.utils.HttpResponseSupport.{errorResponse, successResponse}
import io.circe.syntax.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.{Response, Status}

object NotificationHttpResponses:

  def listResponse(result: domains.notification.application.output.NotificationListResponse): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(result.asJson))

  def unreadCountResponse(result: domains.notification.application.output.NotificationUnreadCountResponse): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(result.asJson))

  def markReadResponse(result: NotificationCommands.MarkNotificationReadResult): IO[Response[IO]] =
    result match
      case NotificationCommands.MarkNotificationReadResult.NotFound => errorResponse(Status.NotFound, ApiMessages.notificationNotFound)
      case NotificationCommands.MarkNotificationReadResult.Marked => successResponse(Status.Ok, ApiMessages.notificationMarkedRead)

  def markAllReadResponse(result: NotificationCommands.MarkAllNotificationsReadResult): IO[Response[IO]] =
    result match
      case NotificationCommands.MarkAllNotificationsReadResult.Marked => successResponse(Status.Ok, ApiMessages.notificationsMarkedRead)
