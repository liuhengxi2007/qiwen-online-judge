package domains.notification.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.notification.objects.{NotificationKind, NotificationPayload}
import domains.notification.objects.request.CreateNotificationRequest
import domains.notification.table.notification.NotificationTable
import domains.user.objects.Username
import org.http4s.Method
import shared.api.ApiPath
import shared.objects.transport.SuccessResponse

import java.sql.Connection

/** 内部通知创建 API，由其他 domain 调用以写入用户通知记录。 */
object CreateNotification extends InternalOnlyApi[CreateNotificationRequest, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/notifications")

  /** 构造通知请求对象，明确收件人、触发人、文案 key、业务 payload 和跳转目标。 */
  def request(
    recipientUsername: Username,
    actorUsername: Option[Username],
    kind: NotificationKind,
    titleKey: String,
    bodyKey: String,
    payload: NotificationPayload,
    targetPath: String,
    targetAnchor: Option[String]
  ): CreateNotificationRequest =
    CreateNotificationRequest(
      recipientUsername = recipientUsername,
      actorUsername = actorUsername,
      kind = kind,
      titleKey = titleKey,
      bodyKey = bodyKey,
      payload = payload,
      targetPath = targetPath,
      targetAnchor = targetAnchor
    )

  /** 将通知写入数据库；不主动推送 SSE，调用方负责在业务事务后发布事件。 */
  override def plan(connection: Connection, request: CreateNotificationRequest): IO[SuccessResponse] =
    NotificationTable
      .insert(
        connection = connection,
        recipientUsername = request.recipientUsername,
        actorUsername = request.actorUsername,
        kind = request.kind,
        titleKey = request.titleKey,
        bodyKey = request.bodyKey,
        payload = request.payload,
        targetPath = request.targetPath,
        targetAnchor = request.targetAnchor
      )
      .as(SuccessResponse(code = None, message = None, params = Map.empty))
