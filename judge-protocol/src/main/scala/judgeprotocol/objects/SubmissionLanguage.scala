package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

enum SubmissionLanguage:
  case Cpp17
  case Python3

object SubmissionLanguage:
  def render(value: SubmissionLanguage): String =
    value match
      case SubmissionLanguage.Cpp17 => "cpp17"
      case SubmissionLanguage.Python3 => "python3"

  given Encoder[SubmissionLanguage] = Encoder.encodeString.contramap(render)
  given Decoder[SubmissionLanguage] = Decoder.decodeString.emap {
    case "cpp17" => Right(SubmissionLanguage.Cpp17)
    case "python3" => Right(SubmissionLanguage.Python3)
    case other => Left(s"Unsupported submission language: $other")
  }
