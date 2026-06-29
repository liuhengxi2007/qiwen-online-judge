package domains.notification.objects.request

import domains.notification.objects.{NotificationKind, NotificationPayload}
import domains.user.objects.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 内部创建通知请求体，包含收件人、触发人、文案 key、payload 和跳转目标。 */
final case class CreateNotificationRequest(
  recipientUsername: Username,
  actorUsername: Option[Username],
  kind: NotificationKind,
  titleKey: String,
  bodyKey: String,
  payload: NotificationPayload,
  targetPath: String,
  targetAnchor: Option[String]
)

/** 提供创建通知请求体 JSON codec。 */
object CreateNotificationRequest:
  given Encoder[CreateNotificationRequest] = deriveEncoder[CreateNotificationRequest]
  given Decoder[CreateNotificationRequest] = deriveDecoder[CreateNotificationRequest]
