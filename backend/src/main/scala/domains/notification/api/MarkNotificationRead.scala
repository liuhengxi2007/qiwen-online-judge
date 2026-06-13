package domains.notification.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.notification.utils.{NotificationEventHub, NotificationEventHubContext, NotificationStreamEvent}
import domains.notification.objects.NotificationId
import domains.notification.table.notification.NotificationTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

/** 标记单条通知已读的认证 API，只允许通知收件人操作。 */
final class MarkNotificationRead(notificationEventHub: NotificationEventHubContext) extends AuthenticatedApi[NotificationId, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/notifications/:notificationId/mark-read")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  /** 从路径解析通知 id，非法 UUID 转为 400。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[NotificationId] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("notificationId").flatMap(NotificationId.parse))

  /** 按通知 id 和当前收件人更新未读状态；不存在、不属于当前用户或已读时返回 404。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    notificationId: NotificationId
  ): IO[SuccessResponse] =
    for
      /** 注意：不属于当前用户的通知返回 404，用于隐藏其他用户通知是否存在；已读通知当前也按未找到处理。 */
      marked <- NotificationTable.markRead(connection, notificationId, actor.username)
      _ <- HttpApiError.ensure(marked, HttpApiError.notFound(ApiMessages.notificationNotFound))
      _ <- NotificationEventHub.publish(notificationEventHub, actor.username, NotificationStreamEvent.NotificationsChanged)
    yield SuccessResponse(
      code = Some(ApiMessages.notificationMarkedRead.code),
      message = None,
      params = ApiMessages.notificationMarkedRead.params
    )
