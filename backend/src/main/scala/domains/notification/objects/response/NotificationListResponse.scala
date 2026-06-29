package domains.notification.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 通知列表分页响应，包含当前页通知、未读数和分页元数据。 */
final case class NotificationListResponse(
  notifications: List[NotificationSummary],
  unreadCount: Int,
  page: Int,
  pageSize: Int,
  totalItems: Long
)

/** 提供通知列表响应 JSON codec。 */
object NotificationListResponse:
  given Encoder[NotificationListResponse] = deriveEncoder[NotificationListResponse]
  given Decoder[NotificationListResponse] = deriveDecoder[NotificationListResponse]
