package domains.auth.objects

import io.circe.{Decoder, Encoder}

final case class PlaintextPassword(value: String)

object PlaintextPassword:
  given Encoder[PlaintextPassword] = Encoder.encodeString.contramap(_.value)
  given Decoder[PlaintextPassword] = Decoder.decodeString.map(PlaintextPassword(_))
