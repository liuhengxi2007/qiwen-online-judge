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

  def fromDatabase(value: String): Option[SubmissionLanguage] =
    value match
      case "cpp17" => Some(SubmissionLanguage.Cpp17)
      case "python3" => Some(SubmissionLanguage.Python3)
      case _ => None

  def toDatabase(value: SubmissionLanguage): String =
    value match
      case SubmissionLanguage.Cpp17 => "cpp17"
      case SubmissionLanguage.Python3 => "python3"
