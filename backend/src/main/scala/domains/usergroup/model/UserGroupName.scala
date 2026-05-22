package domains.usergroup.model



final case class UserGroupName(value: String)

object UserGroupName:
  def parse(raw: String): Either[String, UserGroupName] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("User group name is required.")
    else if normalized.length > 120 then Left("User group name must be at most 120 characters.")
    else Right(UserGroupName(normalized))
