package domains.problem.objects



enum OthersSubmissionAccess:
  case None
  case Summary
  case Detail

object OthersSubmissionAccess:
  def parse(raw: String): Either[String, OthersSubmissionAccess] =
    raw.trim match
      case "none" => Right(OthersSubmissionAccess.None)
      case "summary" => Right(OthersSubmissionAccess.Summary)
      case "detail" => Right(OthersSubmissionAccess.Detail)
      case _ => Left("Other-user submission access must be one of: none, summary, detail.")
