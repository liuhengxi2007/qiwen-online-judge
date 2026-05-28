package domains.user.objects

import io.circe.{Decoder, Encoder}

final case class DisplayName(value: String)

object DisplayName:
  given Encoder[DisplayName] = Encoder.encodeString.contramap(_.value)
  given Decoder[DisplayName] = Decoder.decodeString.map(DisplayName(_))
