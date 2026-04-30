package domains.notification.model

import io.circe.{Decoder, Encoder}

import java.util.UUID
import scala.util.Try

final case class NotificationId(value: UUID)

object NotificationId:
  def random(): NotificationId = NotificationId(UUID.randomUUID())

  def parse(raw: String): Either[String, NotificationId] =
    Try(UUID.fromString(raw.trim)).toEither.left.map(_.getMessage).map(NotificationId(_))

  given Encoder[NotificationId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[NotificationId] = Decoder.decodeString.emap(parse)
