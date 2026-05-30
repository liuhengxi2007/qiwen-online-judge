package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

final case class TestcaseName(value: String)

object TestcaseName:
  def parse(raw: String): Either[String, TestcaseName] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Testcase name is required.")
    else if normalized.length > 120 then Left("Testcase name must be at most 120 characters.")
    else Right(TestcaseName(normalized))

  given Encoder[TestcaseName] = Encoder.encodeString.contramap(_.value)
  given Decoder[TestcaseName] = Decoder.decodeString.emap(parse)
