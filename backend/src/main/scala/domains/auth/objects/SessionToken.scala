package domains.auth.objects



final case class SessionToken(value: String)

object SessionToken:
  def parse(raw: String): Either[String, SessionToken] =
    val normalized = raw.trim
    if normalized.isEmpty then Left("Session token is required.")
    else Right(SessionToken(normalized))
