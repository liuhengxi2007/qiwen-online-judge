package domains.submission.model



final case class SubmissionSourceCode(value: String)

object SubmissionSourceCode:
  def parse(raw: String): Either[String, SubmissionSourceCode] =
    if raw.trim.isEmpty then Left("Source code is required.")
    else if raw.length > 200000 then Left("Source code must be at most 200000 characters.")
    else Right(SubmissionSourceCode(raw))
