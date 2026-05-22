package domains.notification.http.response

import domains.notification.model.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class NotificationUnreadCountResponse(unreadCount: Int)

object NotificationUnreadCountResponse:
  given Encoder[NotificationUnreadCountResponse] = deriveEncoder[NotificationUnreadCountResponse]
  given Decoder[NotificationUnreadCountResponse] = deriveDecoder[NotificationUnreadCountResponse]
