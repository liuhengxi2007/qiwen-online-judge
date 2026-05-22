package domains.notification.http.codec

import domains.notification.application.output.*
import domains.notification.http.codec.NotificationModelHttpCodecs.given
import domains.user.http.codec.UserModelHttpCodecs.given
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

object NotificationHttpCodecs:
  export NotificationModelHttpCodecs.given

  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[NotificationSummary] = deriveEncoder[NotificationSummary]
  given Decoder[NotificationSummary] = deriveDecoder[NotificationSummary]
  given Encoder[NotificationListResponse] = deriveEncoder[NotificationListResponse]
  given Decoder[NotificationListResponse] = deriveDecoder[NotificationListResponse]
  given Encoder[NotificationUnreadCountResponse] = deriveEncoder[NotificationUnreadCountResponse]
  given Decoder[NotificationUnreadCountResponse] = deriveDecoder[NotificationUnreadCountResponse]
