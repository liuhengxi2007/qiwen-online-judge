package domains.auth.model

import io.circe.{Decoder, Encoder}

final case class SessionToken(value: String)

object SessionToken:
  def parse(raw: String): Either[String, SessionToken] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Session token is required.")
    else Right(SessionToken(normalized))

  given Encoder[SessionToken] = Encoder.encodeString.contramap(_.value)
  given Decoder[SessionToken] = Decoder.decodeString.emap(parse)
