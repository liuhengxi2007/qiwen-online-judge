package domains.submission.model



enum SubmissionLanguage:
  case Cpp17
  case Python3

object SubmissionLanguage:
  def parse(value: String): Either[String, SubmissionLanguage] =
    value.trim match
      case "cpp17" => Right(SubmissionLanguage.Cpp17)
      case "python3" => Right(SubmissionLanguage.Python3)
      case _ => Left("Submission language must be one of: cpp17, python3.")
