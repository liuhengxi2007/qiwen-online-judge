package domains.submission.application.input



enum SubmissionSort:
  case Submitted
  case Time
  case Memory
  case CodeLength

object SubmissionSort:
  def parse(value: String): Either[String, SubmissionSort] =
    value.trim match
      case "submitted" => Right(SubmissionSort.Submitted)
      case "time" => Right(SubmissionSort.Time)
      case "memory" => Right(SubmissionSort.Memory)
      case "code_length" => Right(SubmissionSort.CodeLength)
      case _ =>
        Left("Submission sort must be one of: submitted, time, memory, code_length.")

  def toDatabase(value: SubmissionSort): String =
    value match
      case SubmissionSort.Submitted => "submitted"
      case SubmissionSort.Time => "time"
      case SubmissionSort.Memory => "memory"
      case SubmissionSort.CodeLength => "code_length"

  def defaultDirection(value: SubmissionSort): SubmissionSortDirection =
    value match
      case SubmissionSort.Submitted => SubmissionSortDirection.Desc
      case SubmissionSort.Time => SubmissionSortDirection.Asc
      case SubmissionSort.Memory => SubmissionSortDirection.Asc
      case SubmissionSort.CodeLength => SubmissionSortDirection.Asc
