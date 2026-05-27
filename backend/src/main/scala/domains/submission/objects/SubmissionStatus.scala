package domains.submission.objects



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
