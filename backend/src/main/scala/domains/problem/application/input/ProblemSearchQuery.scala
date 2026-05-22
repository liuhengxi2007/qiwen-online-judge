package domains.problem.application.input



final case class ProblemSearchQuery(value: String)

object ProblemSearchQuery:
  def parse(raw: String): Either[String, ProblemSearchQuery] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem search query is required.")
    else Right(ProblemSearchQuery(normalized))
