package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

final case class ProblemSpaceLimitMb(value: Int)

object ProblemSpaceLimitMb:
  def parse(raw: Int): Either[String, ProblemSpaceLimitMb] =
    if raw < 1 then Left("Problem space limit must be greater than 0.")
    else if raw > 65536 then Left("Problem space limit must be at most 65536 MB.")
    else Right(ProblemSpaceLimitMb(raw))

  given Encoder[ProblemSpaceLimitMb] = Encoder.encodeInt.contramap(_.value)
  given Decoder[ProblemSpaceLimitMb] = Decoder.decodeInt.emap(parse)
