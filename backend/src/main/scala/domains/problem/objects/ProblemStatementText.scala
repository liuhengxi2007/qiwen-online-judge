package domains.problem.objects

import io.circe.{Decoder, Encoder}


final case class ProblemStatementText(value: String)

object ProblemStatementText:
  given Encoder[ProblemStatementText] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemStatementText] = Decoder.decodeString.emap(parse)

  def parse(raw: String): Either[String, ProblemStatementText] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem statement is required.")
    else if normalized.length > 20000 then Left("Problem statement must be at most 20000 characters.")
    else Right(ProblemStatementText(normalized))
