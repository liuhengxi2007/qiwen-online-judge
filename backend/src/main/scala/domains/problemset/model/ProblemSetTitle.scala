package domains.problemset.model



final case class ProblemSetTitle(value: String)

object ProblemSetTitle:
  def parse(raw: String): Either[String, ProblemSetTitle] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem set title is required.")
    else if normalized.length > 120 then Left("Problem set title must be at most 120 characters.")
    else Right(ProblemSetTitle(normalized))
