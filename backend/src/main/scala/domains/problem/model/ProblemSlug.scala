package domains.problem.model



final case class ProblemSlug(value: String)

object ProblemSlug:
  private val slugPattern = "^[a-z0-9]+(?:-[a-z0-9]+)*$".r

  def parse(raw: String): Either[String, ProblemSlug] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem slug is required.")
    else if normalized.length < 3 || normalized.length > 64 then Left("Problem slug must be between 3 and 64 characters.")
    else if !slugPattern.matches(normalized) then Left("Problem slug may contain only lowercase letters, numbers, and hyphens.")
    else Right(ProblemSlug(normalized))
