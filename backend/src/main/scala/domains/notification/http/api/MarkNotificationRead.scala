package domains.notification.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.notification.application.{NotificationEventHub, NotificationStreamEvent}
import domains.notification.objects.NotificationId
import domains.notification.table.notification.NotificationTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.codec.SharedHttpCodecs.given
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

final class MarkNotificationRead(notificationEventHub: NotificationEventHub) extends AuthenticatedApi[NotificationId, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/notifications/:notificationId/read")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[NotificationId] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("notificationId").flatMap(NotificationId.parse))

  override def plan(
    connection: Connection,
    actor: AuthUser,
    notificationId: NotificationId
  ): IO[SuccessResponse] =
    for
      marked <- NotificationTable.markRead(connection, notificationId, actor.username)
      _ <- HttpApiError.ensure(marked, HttpApiError.notFound(ApiMessages.notificationNotFound))
      _ <- notificationEventHub.publish(actor.username, NotificationStreamEvent.NotificationsChanged)
    yield SuccessResponse(
      code = Some(ApiMessages.notificationMarkedRead.code),
      message = None,
      params = ApiMessages.notificationMarkedRead.params
    )
