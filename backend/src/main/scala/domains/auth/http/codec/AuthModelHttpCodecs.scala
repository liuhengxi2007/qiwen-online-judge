package domains.auth.http.codec

import domains.auth.objects.{EmailAddress, PlaintextPassword}
import io.circe.{Decoder, Encoder}

object AuthModelHttpCodecs:
  given Encoder[EmailAddress] = Encoder.encodeString.contramap(_.value)
  given Decoder[EmailAddress] = Decoder.decodeString.map(value => EmailAddress(value))

  given Encoder[PlaintextPassword] = Encoder.encodeString.contramap(_.value)
  given Decoder[PlaintextPassword] = Decoder.decodeString.map(value => PlaintextPassword(value))
