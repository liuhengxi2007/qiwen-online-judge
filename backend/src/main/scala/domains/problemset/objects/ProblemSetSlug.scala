package domains.problemset.objects



final case class ProblemSetSlug(value: String)

object ProblemSetSlug:
  private val slugPattern = "^[a-z0-9]+(?:-[a-z0-9]+)*$".r

  def parse(raw: String): Either[String, ProblemSetSlug] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Problem set slug is required.")
    else if normalized.length < 3 || normalized.length > 64 then Left("Problem set slug must be between 3 and 64 characters.")
    else if !slugPattern.matches(normalized) then Left("Problem set slug may contain only lowercase letters, numbers, and hyphens.")
    else Right(ProblemSetSlug(normalized))
