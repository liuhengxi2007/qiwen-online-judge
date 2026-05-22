package domains.auth.model



import io.circe.{Decoder, Encoder}

final case class EmailAddress(value: String)

object EmailAddress:
  given Encoder[EmailAddress] = Encoder.encodeString.contramap(_.value)
  given Decoder[EmailAddress] = Decoder.decodeString.map(value => EmailAddress(value))

final case class PlaintextPassword(value: String)

object PlaintextPassword:
  given Encoder[PlaintextPassword] = Encoder.encodeString.contramap(_.value)
  given Decoder[PlaintextPassword] = Decoder.decodeString.map(value => PlaintextPassword(value))

final case class PasswordHash(value: String)

object PasswordHash:
  given Encoder[PasswordHash] = Encoder.encodeString.contramap(_.value)
  given Decoder[PasswordHash] = Decoder.decodeString.map(value => PasswordHash(value))
