package auth

object UsernameRules:

  private val usernamePattern = "^[A-Za-z0-9_-]+$".r

  def validate(username: String): Option[String] =
    val normalized = username.trim

    if normalized.length < 3 || normalized.length > 32 then
      Some("Username must be between 3 and 32 characters.")
    else if !usernamePattern.matches(normalized) then
      Some("Username may contain only letters, numbers, underscores, and hyphens.")
    else
      None
