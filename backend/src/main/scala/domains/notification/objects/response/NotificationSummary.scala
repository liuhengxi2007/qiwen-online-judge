package domains.notification.objects.response

import domains.notification.objects.*

import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.Instant
import scala.util.Try

final case class NotificationSummary(
  id: NotificationId,
  kind: NotificationKind,
  actor: Option[UserIdentity],
  titleKey: String,
  bodyKey: String,
  payload: NotificationPayload,
  targetPath: String,
  targetAnchor: Option[String],
  isRead: Boolean,
  createdAt: Instant
)

object NotificationSummary:
  private given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  private given Decoder[Instant] = Decoder.decodeString.emap { value =>
    Try(Instant.parse(value)).toEither.left.map(_.getMessage)
  }

  given Encoder[NotificationSummary] = deriveEncoder[NotificationSummary]
  given Decoder[NotificationSummary] = deriveDecoder[NotificationSummary]
