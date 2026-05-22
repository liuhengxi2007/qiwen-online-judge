package domains.auth.application



import domains.user.model.Username

object UsernameRules:

  private val usernamePattern = "^[a-z0-9_-]+$".r

  def validate(username: Username): Option[String] =
    val normalized = Username.normalize(username.value)

    if normalized.length < 3 || normalized.length > 32 then
      Some("Username must be between 3 and 32 characters.")
    else if !usernamePattern.matches(normalized) then
      Some("Username may contain only lowercase letters, numbers, underscores, and hyphens.")
    else
      None
