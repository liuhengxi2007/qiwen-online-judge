package domains.problem.objects

import io.circe.{Decoder, Encoder}


final case class ProblemSpaceLimitMb(value: Int)

object ProblemSpaceLimitMb:
  given Encoder[ProblemSpaceLimitMb] = Encoder.encodeInt.contramap(_.value)
  given Decoder[ProblemSpaceLimitMb] = Decoder.decodeInt.emap(parse)

  def parse(raw: Int): Either[String, ProblemSpaceLimitMb] =
    if raw < 1 then Left("Problem space limit must be at least 1 MB.")
    else if raw > 65536 then Left("Problem space limit must be at most 65536 MB.")
    else Right(ProblemSpaceLimitMb(raw))
