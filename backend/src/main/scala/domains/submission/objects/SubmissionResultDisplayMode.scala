package domains.submission.objects

import io.circe.{Decoder, Encoder}

enum SubmissionResultDisplayMode:
  case Verdict
  case Score

object SubmissionResultDisplayMode:
  given Encoder[SubmissionResultDisplayMode] = Encoder.encodeString.contramap(encode)
  given Decoder[SubmissionResultDisplayMode] = Decoder.decodeString.emap(parse)

  def parse(value: String): Either[String, SubmissionResultDisplayMode] =
    value.trim match
      case "verdict" => Right(SubmissionResultDisplayMode.Verdict)
      case "score" => Right(SubmissionResultDisplayMode.Score)
      case _ => Left("Submission result display mode must be one of: verdict, score.")

  def encode(value: SubmissionResultDisplayMode): String =
    value match
      case SubmissionResultDisplayMode.Verdict => "verdict"
      case SubmissionResultDisplayMode.Score => "score"
