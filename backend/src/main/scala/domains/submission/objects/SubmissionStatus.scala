package domains.submission.objects

import io.circe.{Decoder, Encoder}


enum SubmissionStatus:
  case Queued
  case Running
  case Completed
  case Failed

object SubmissionStatus:
  given Encoder[SubmissionStatus] = Encoder.encodeString.contramap(encode)
  given Decoder[SubmissionStatus] = Decoder.decodeString.emap(parse)

  def parse(value: String): Either[String, SubmissionStatus] =
    value.trim match
      case "queued" => Right(SubmissionStatus.Queued)
      case "running" => Right(SubmissionStatus.Running)
      case "completed" => Right(SubmissionStatus.Completed)
      case "failed" => Right(SubmissionStatus.Failed)
      case _ => Left("Submission status must be one of: queued, running, completed, failed.")

  private def encode(value: SubmissionStatus): String =
    value match
      case SubmissionStatus.Queued => "queued"
      case SubmissionStatus.Running => "running"
      case SubmissionStatus.Completed => "completed"
      case SubmissionStatus.Failed => "failed"
