package domains.submission.application.input



final case class SubmissionUserQuery(value: String)

object SubmissionUserQuery:
  def parse(raw: String): Either[String, SubmissionUserQuery] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Submission username query is required.")
    else Right(SubmissionUserQuery(normalized))
