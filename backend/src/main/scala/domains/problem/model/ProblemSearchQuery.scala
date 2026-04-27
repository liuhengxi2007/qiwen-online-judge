package domains.problem.model

import io.circe.{Decoder, Encoder}

final case class ProblemSearchQuery(value: String)

object ProblemSearchQuery:
  def parse(raw: String): Either[String, ProblemSearchQuery] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem search query is required.")
    else Right(ProblemSearchQuery(normalized))

  given Encoder[ProblemSearchQuery] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSearchQuery] = Decoder.decodeString.emap(parse)
