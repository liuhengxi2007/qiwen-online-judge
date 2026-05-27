package domains.user.objects.request



final case class UserSearchQuery(value: String)

object UserSearchQuery:
  def parse(raw: String): Either[String, UserSearchQuery] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("User search query is required.")
    else Right(UserSearchQuery(normalized))
