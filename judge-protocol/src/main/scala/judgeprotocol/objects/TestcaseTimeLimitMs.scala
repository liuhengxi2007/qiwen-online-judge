package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

final case class TestcaseTimeLimitMs(value: Int)

object TestcaseTimeLimitMs:
  def parse(raw: Int): Either[String, TestcaseTimeLimitMs] =
    if raw < 1 then Left("Testcase time limit must be greater than 0.")
    else if raw > 600000 then Left("Testcase time limit must be at most 600000 ms.")
    else Right(TestcaseTimeLimitMs(raw))

  given Encoder[TestcaseTimeLimitMs] = Encoder.encodeInt.contramap(_.value)
  given Decoder[TestcaseTimeLimitMs] = Decoder.decodeInt.emap(parse)
