package domains.auth.model

import io.circe.{Decoder, Encoder}
import java.util.Locale

final case class Username(value: String)

object Username:
  def normalize(raw: String): String =
    raw.trim.toLowerCase(Locale.ROOT)

  def canonical(raw: String): Username =
    new Username(normalize(raw))

  given Encoder[Username] = Encoder.encodeString.contramap(_.value)
  given Decoder[Username] = Decoder.decodeString.map(canonical)

final case class DisplayName(value: String)

object DisplayName:
  given Encoder[DisplayName] = Encoder.encodeString.contramap(_.value)
  given Decoder[DisplayName] = Decoder.decodeString.map(value => DisplayName(value))

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
