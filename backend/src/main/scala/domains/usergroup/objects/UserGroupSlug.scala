package domains.usergroup.objects



final case class UserGroupSlug(value: String)

object UserGroupSlug:
  private val slugPattern = "^[a-z0-9]+(?:-[a-z0-9]+)*$".r

  def parse(raw: String): Either[String, UserGroupSlug] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("User group slug is required.")
    else if normalized.length < 3 || normalized.length > 64 then Left("User group slug must be between 3 and 64 characters.")
    else if !slugPattern.matches(normalized) then Left("User group slug may contain only lowercase letters, numbers, and hyphens.")
    else Right(UserGroupSlug(normalized))
