package domains.notification.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.notification.http.codec.NotificationHttpCodecs.given
import domains.notification.objects.response.NotificationUnreadCountResponse
import domains.notification.table.notification.NotificationTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.{ApiPath, PathParams}

import java.sql.Connection

object GetNotificationUnreadCount extends AuthenticatedApi[Unit, NotificationUnreadCountResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/notifications/unread-count")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[NotificationUnreadCountResponse] = summon[Encoder[NotificationUnreadCountResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = (request, pathParams)
    IO.unit

  override def plan(
    connection: Connection,
    actor: AuthUser,
    input: Unit
  ): IO[NotificationUnreadCountResponse] =
    val _ = input
    NotificationTable.countUnreadForRecipient(connection, actor.username).map(NotificationUnreadCountResponse(_))
