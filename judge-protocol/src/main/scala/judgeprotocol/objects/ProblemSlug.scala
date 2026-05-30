package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

final case class ProblemSlug(value: String)

object ProblemSlug:
  given Encoder[ProblemSlug] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSlug] = Decoder.decodeString.emap { value =>
    val normalized = value.trim
    if normalized.isEmpty then Left("Problem slug is required.") else Right(ProblemSlug(normalized))
  }
