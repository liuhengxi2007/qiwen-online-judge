package domains.problem.model



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

  def fromDatabase(value: String): Option[OthersSubmissionAccess] =
    value match
      case "none" => scala.Some(OthersSubmissionAccess.None)
      case "summary" => scala.Some(OthersSubmissionAccess.Summary)
      case "detail" => scala.Some(OthersSubmissionAccess.Detail)
      case _ => scala.None

  def toDatabase(value: OthersSubmissionAccess): String =
    value match
      case OthersSubmissionAccess.None => "none"
      case OthersSubmissionAccess.Summary => "summary"
      case OthersSubmissionAccess.Detail => "detail"
