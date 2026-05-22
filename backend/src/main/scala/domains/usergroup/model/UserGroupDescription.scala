package domains.usergroup.model



final case class UserGroupDescription(value: String)

object UserGroupDescription:
  def parse(raw: String): Either[String, UserGroupDescription] =
    val normalized = raw.trim
    if normalized.length > 2000 then Left("User group description must be at most 2000 characters.")
    else Right(UserGroupDescription(normalized))
