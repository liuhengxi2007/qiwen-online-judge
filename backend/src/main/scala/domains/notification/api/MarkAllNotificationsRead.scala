package domains.notification.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.notification.utils.{NotificationEventHub, NotificationStreamEvent}
import domains.notification.table.notification.NotificationTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

final class MarkAllNotificationsRead(notificationEventHub: NotificationEventHub) extends AuthenticatedApi[Unit, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/notifications/read-all")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = (request, pathParams)
    IO.unit

  override def plan(
    connection: Connection,
    actor: AuthUser,
    input: Unit
  ): IO[SuccessResponse] =
    val _ = input
    for
      _ <- NotificationTable.markAllRead(connection, actor.username)
      _ <- notificationEventHub.publish(actor.username, NotificationStreamEvent.NotificationsChanged)
    yield SuccessResponse(
      code = Some(ApiMessages.notificationsMarkedRead.code),
      message = None,
      params = ApiMessages.notificationsMarkedRead.params
    )
