package domains.notification.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class NotificationListResponse(
  notifications: List[NotificationSummary],
  unreadCount: Int,
  page: Int,
  pageSize: Int,
  totalItems: Long
)

object NotificationListResponse:
  given Encoder[NotificationListResponse] = deriveEncoder[NotificationListResponse]
  given Decoder[NotificationListResponse] = deriveDecoder[NotificationListResponse]
