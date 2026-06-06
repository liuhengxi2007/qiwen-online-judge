package domains.submission.objects

import io.circe.{Decoder, Encoder}


enum SubmissionLanguage:
  case Cpp17
  case Python3
  case Text

object SubmissionLanguage:
  given Encoder[SubmissionLanguage] = Encoder.encodeString.contramap(encode)
  given Decoder[SubmissionLanguage] = Decoder.decodeString.emap(parse)

  def parse(value: String): Either[String, SubmissionLanguage] =
    value.trim match
      case "cpp17" => Right(SubmissionLanguage.Cpp17)
      case "python3" => Right(SubmissionLanguage.Python3)
      case "text" => Right(SubmissionLanguage.Text)
      case _ => Left("Submission language must be one of: cpp17, python3, text.")

  private def encode(value: SubmissionLanguage): String =
    value match
      case SubmissionLanguage.Cpp17 => "cpp17"
      case SubmissionLanguage.Python3 => "python3"
      case SubmissionLanguage.Text => "text"
