package domains.notification.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.notification.objects.response.NotificationUnreadCountResponse
import domains.notification.table.notification.NotificationTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, PathParams}

import java.sql.Connection

/** 获取当前用户未读通知数量的认证 API。 */
object GetNotificationUnreadCount extends AuthenticatedApi[Unit, NotificationUnreadCountResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/notifications/unread-count")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[NotificationUnreadCountResponse] = summon[Encoder[NotificationUnreadCountResponse]]

  /** 未读数查询不需要路径参数或请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[Unit] =
    val _ = (request, pathParams)
    IO.unit

  /** 按当前用户统计未读通知数量。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: Unit
  ): IO[NotificationUnreadCountResponse] =
    val _ = input
    NotificationTable.countUnreadForRecipient(connection, actor.username).map(NotificationUnreadCountResponse(_))
