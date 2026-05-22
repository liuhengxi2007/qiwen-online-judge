package domains.user.model

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
