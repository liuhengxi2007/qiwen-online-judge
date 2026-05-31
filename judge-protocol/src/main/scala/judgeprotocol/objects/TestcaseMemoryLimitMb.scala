package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

final case class TestcaseMemoryLimitMb(value: Int)

object TestcaseMemoryLimitMb:
  def parse(raw: Int): Either[String, TestcaseMemoryLimitMb] =
    if raw < 1 then Left("Testcase memory limit must be greater than 0.")
    else if raw > 65536 then Left("Testcase memory limit must be at most 65536 MB.")
    else Right(TestcaseMemoryLimitMb(raw))

  given Encoder[TestcaseMemoryLimitMb] = Encoder.encodeInt.contramap(_.value)
  given Decoder[TestcaseMemoryLimitMb] = Decoder.decodeInt.emap(parse)
