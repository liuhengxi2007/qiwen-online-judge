package domains.submission.model



import io.circe.{Decoder, Encoder}

enum SubmissionStatus:
  case Queued
  case Running
  case Completed
  case Failed

object SubmissionStatus:
  def parse(value: String): Either[String, SubmissionStatus] =
    value.trim match
      case "queued" => Right(SubmissionStatus.Queued)
      case "running" => Right(SubmissionStatus.Running)
      case "completed" => Right(SubmissionStatus.Completed)
      case "failed" => Right(SubmissionStatus.Failed)
      case _ => Left("Submission status must be one of: queued, running, completed, failed.")

  def fromDatabase(value: String): Option[SubmissionStatus] =
    value match
      case "queued" => Some(SubmissionStatus.Queued)
      case "running" => Some(SubmissionStatus.Running)
      case "completed" => Some(SubmissionStatus.Completed)
      case "failed" => Some(SubmissionStatus.Failed)
      case _ => None

  def toDatabase(value: SubmissionStatus): String =
    value match
      case SubmissionStatus.Queued => "queued"
      case SubmissionStatus.Running => "running"
      case SubmissionStatus.Completed => "completed"
      case SubmissionStatus.Failed => "failed"

  given Encoder[SubmissionStatus] = Encoder.encodeString.contramap(toDatabase)
  given Decoder[SubmissionStatus] = Decoder.decodeString.emap(parse)
