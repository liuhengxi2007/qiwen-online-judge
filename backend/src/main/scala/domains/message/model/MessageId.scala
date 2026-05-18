package domains.message.model

import io.circe.{Decoder, Encoder}

import java.util.UUID
import scala.util.Try

final case class MessageId(value: UUID)

object MessageId:

  def parse(raw: String): Either[String, MessageId] =
    Try(UUID.fromString(raw.trim)).toEither.left.map(_.getMessage).map(MessageId(_))

  given Encoder[MessageId] = Encoder.encodeString.contramap(_.value.toString)
  given Decoder[MessageId] = Decoder.decodeString.emap(parse)
