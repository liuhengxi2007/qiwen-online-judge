package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

final case class ProblemTimeLimitMs(value: Int)

object ProblemTimeLimitMs:
  def parse(raw: Int): Either[String, ProblemTimeLimitMs] =
    if raw < 1 then Left("Problem time limit must be greater than 0.")
    else if raw > 600000 then Left("Problem time limit must be at most 600000 ms.")
    else Right(ProblemTimeLimitMs(raw))

  given Encoder[ProblemTimeLimitMs] = Encoder.encodeInt.contramap(_.value)
  given Decoder[ProblemTimeLimitMs] = Decoder.decodeInt.emap(parse)
