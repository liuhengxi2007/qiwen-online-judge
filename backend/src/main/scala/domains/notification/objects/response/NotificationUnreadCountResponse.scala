package domains.notification.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 未读通知数量响应。 */
final case class NotificationUnreadCountResponse(unreadCount: Int)

/** 提供未读通知数量响应 JSON codec。 */
object NotificationUnreadCountResponse:
  given Encoder[NotificationUnreadCountResponse] = deriveEncoder[NotificationUnreadCountResponse]
  given Decoder[NotificationUnreadCountResponse] = deriveDecoder[NotificationUnreadCountResponse]
