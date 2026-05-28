package domains.message.objects

import io.circe.{Decoder, Encoder}


final case class MessageContent(value: String)

object MessageContent:
  given Encoder[MessageContent] = Encoder.encodeString.contramap(_.value)
  given Decoder[MessageContent] = Decoder.decodeString.emap(parse)

  private val maxLength = 5000

  def parse(raw: String): Either[String, MessageContent] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Message content is required.")
    else if normalized.length > maxLength then Left(s"Message content must be at most $maxLength characters.")
    else Right(MessageContent(normalized))
