package judgeprotocol.objects.response

import io.circe.{Decoder, Encoder}

enum JudgeTestcaseType:
  case Main
  case Sample
  case Hack

object JudgeTestcaseType:
  given Encoder[JudgeTestcaseType] = Encoder.encodeString.contramap(render)
  given Decoder[JudgeTestcaseType] = Decoder.decodeString.emap(parse)

  def render(value: JudgeTestcaseType): String =
    value match
      case Main => "main"
      case Sample => "sample"
      case Hack => "hack"

  def parse(raw: String): Either[String, JudgeTestcaseType] =
    raw.trim match
      case "main" => Right(Main)
      case "sample" => Right(Sample)
      case "hack" => Right(Hack)
      case other => Left(s"Unsupported testcase type: $other.")
