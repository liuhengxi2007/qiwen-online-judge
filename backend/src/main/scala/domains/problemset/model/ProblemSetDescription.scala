package domains.problemset.model



final case class ProblemSetDescription(value: String)

object ProblemSetDescription:
  def parse(raw: String): Either[String, ProblemSetDescription] =
    val normalized = raw.trim
    if normalized.length > 2000 then Left("Problem set description must be at most 2000 characters.")
    else Right(ProblemSetDescription(normalized))
