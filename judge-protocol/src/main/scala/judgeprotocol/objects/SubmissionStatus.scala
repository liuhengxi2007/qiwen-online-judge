package judgeprotocol.objects

import io.circe.{Decoder, Encoder}

enum SubmissionStatus:
  case Queued
  case Running
  case Completed
  case Failed

object SubmissionStatus:
  def render(value: SubmissionStatus): String =
    value match
      case SubmissionStatus.Queued => "queued"
      case SubmissionStatus.Running => "running"
      case SubmissionStatus.Completed => "completed"
      case SubmissionStatus.Failed => "failed"

  given Encoder[SubmissionStatus] = Encoder.encodeString.contramap(render)
  given Decoder[SubmissionStatus] = Decoder.decodeString.emap {
    case "queued" => Right(SubmissionStatus.Queued)
    case "running" => Right(SubmissionStatus.Running)
    case "completed" => Right(SubmissionStatus.Completed)
    case "failed" => Right(SubmissionStatus.Failed)
    case other => Left(s"Unsupported submission status: $other")
  }
