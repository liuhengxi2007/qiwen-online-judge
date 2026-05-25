package domains.submission.model.request



final case class SubmissionProblemQuery(value: String)

object SubmissionProblemQuery:
  def parse(raw: String): Either[String, SubmissionProblemQuery] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Submission problem query is required.")
    else Right(SubmissionProblemQuery(normalized))
