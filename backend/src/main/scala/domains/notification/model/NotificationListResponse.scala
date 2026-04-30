package domains.notification.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class NotificationListResponse(
  notifications: List[NotificationSummary],
  unreadCount: Int
)

object NotificationListResponse:
  given Encoder[NotificationListResponse] = deriveEncoder[NotificationListResponse]
  given Decoder[NotificationListResponse] = deriveDecoder[NotificationListResponse]
