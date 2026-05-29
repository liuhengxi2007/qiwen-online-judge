package domains.notification.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.notification.objects.{NotificationKind, NotificationPayload}
import domains.notification.objects.request.CreateNotificationRequest
import domains.notification.table.notification.NotificationTable
import domains.user.objects.Username
import org.http4s.Method
import shared.api.ApiPath
import shared.objects.response.SuccessResponse

import java.sql.Connection

object CreateNotification extends InternalOnlyApi[CreateNotificationRequest, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/notifications")

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
