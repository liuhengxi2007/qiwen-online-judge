package domains.problemset.model



import io.circe.{Decoder, Encoder}

final case class ProblemSetTitle(value: String)

object ProblemSetTitle:
  def parse(raw: String): Either[String, ProblemSetTitle] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem set title is required.")
    else if normalized.length > 120 then Left("Problem set title must be at most 120 characters.")
    else Right(ProblemSetTitle(normalized))

  given Encoder[ProblemSetTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSetTitle] = Decoder.decodeString.emap(parse)
