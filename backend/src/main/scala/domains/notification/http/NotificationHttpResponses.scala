package domains.notification.http

import cats.effect.IO
import domains.notification.application.NotificationCommands
import domains.shared.http.ApiMessages
import domains.shared.http.HttpResponseSupport.{errorResponse, successResponse}
import io.circe.syntax.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.{Response, Status}

object NotificationHttpResponses:

  def listResponse(result: domains.notification.model.NotificationListResponse): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(result.asJson))

  def unreadCountResponse(result: domains.notification.model.NotificationUnreadCountResponse): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Ok).withEntity(result.asJson))

  def markReadResponse(result: NotificationCommands.MarkNotificationReadResult): IO[Response[IO]] =
    result match
      case NotificationCommands.MarkNotificationReadResult.NotFound => errorResponse(Status.NotFound, ApiMessages.notificationNotFound)
      case NotificationCommands.MarkNotificationReadResult.Marked => successResponse(Status.Ok, ApiMessages.notificationMarkedRead)

  def markAllReadResponse(result: NotificationCommands.MarkAllNotificationsReadResult): IO[Response[IO]] =
    result match
      case NotificationCommands.MarkAllNotificationsReadResult.Marked => successResponse(Status.Ok, ApiMessages.notificationsMarkedRead)
