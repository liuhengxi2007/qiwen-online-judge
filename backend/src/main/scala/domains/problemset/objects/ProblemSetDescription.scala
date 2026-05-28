package domains.problemset.objects

import io.circe.{Decoder, Encoder}


final case class ProblemSetDescription(value: String)

object ProblemSetDescription:
  given Encoder[ProblemSetDescription] = Encoder.encodeString.contramap(_.value)
  given Decoder[ProblemSetDescription] = Decoder.decodeString.emap(parse)

  def parse(raw: String): Either[String, ProblemSetDescription] =
    val normalized = raw.trim
    if normalized.length > 2000 then Left("Problem set description must be at most 2000 characters.")
    else Right(ProblemSetDescription(normalized))
