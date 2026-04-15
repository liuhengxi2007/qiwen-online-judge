package domains.problem.model

import io.circe.{Decoder, Encoder}

final case class ProblemTitle(value: String)

object ProblemTitle:
  def parse(raw: String): Either[String, ProblemTitle] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem title is required.")
    else if normalized.length > 120 then Left("Problem title must be at most 120 characters.")
    else Right(ProblemTitle(normalized))

  given Encoder[ProblemTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemTitle] = Decoder.decodeString.emap(parse)
