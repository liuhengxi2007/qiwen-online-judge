package domains.notification.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.notification.table.notification.NotificationTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, PathParams}
import shared.objects.transport.SuccessResponse

import java.sql.Connection

/** 标记当前用户所有通知已读的认证 API，完成后发布通知流变更事件。 */
final class MarkAllNotificationsRead(notificationEventHub: NotificationEventHubContext) extends AuthenticatedApi[Unit, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/notifications/mark-all-read")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  /** 批量已读操作不需要路径参数或请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = (request, pathParams)
    IO.unit

  /** 将当前用户所有未读通知置为已读，并向该用户 SSE 订阅发布变更。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: Unit
  ): IO[SuccessResponse] =
    val _ = input
    for
      _ <- NotificationTable.markAllRead(connection, actor.username)
      _ <- NotificationEventHub.publish(notificationEventHub, actor.username, NotificationStreamEvent.NotificationsChanged)
    yield SuccessResponse(
      code = Some(ApiMessages.notificationsMarkedRead.code),
      message = None,
      params = ApiMessages.notificationsMarkedRead.params
    )
