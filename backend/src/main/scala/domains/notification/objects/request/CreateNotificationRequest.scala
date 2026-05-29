package domains.notification.objects.request

import domains.notification.objects.{NotificationKind, NotificationPayload}
import domains.user.objects.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

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

object CreateNotificationRequest:
  given Encoder[CreateNotificationRequest] = deriveEncoder[CreateNotificationRequest]
  given Decoder[CreateNotificationRequest] = deriveDecoder[CreateNotificationRequest]
