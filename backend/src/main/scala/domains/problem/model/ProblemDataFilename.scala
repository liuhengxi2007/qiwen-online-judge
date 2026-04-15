package domains.problem.model

import io.circe.{Decoder, Encoder}

final case class ProblemDataFilename(value: String)

object ProblemDataFilename:
  def parse(raw: String): Either[String, ProblemDataFilename] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem data file name is required.")
    else if normalized.length > 255 then Left("Problem data file name must be at most 255 characters.")
    else Right(ProblemDataFilename(normalized))

  given Encoder[ProblemDataFilename] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemDataFilename] = Decoder.decodeString.emap(parse)
