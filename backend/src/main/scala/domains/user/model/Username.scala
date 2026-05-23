package domains.user.model

import java.util.Locale

final case class Username(value: String)

object Username:
  private val usernamePattern = "^[a-z0-9_-]+$".r

  def normalize(raw: String): String =
    raw.trim.toLowerCase(Locale.ROOT)

  def canonical(raw: String): Username =
    new Username(normalize(raw))

  def parse(raw: String): Either[String, Username] =
    val normalized = normalize(raw)

    if normalized.length < 3 || normalized.length > 32 then
      Left("Username must be between 3 and 32 characters.")
    else if !usernamePattern.matches(normalized) then
      Left("Username may contain only lowercase letters, numbers, underscores, and hyphens.")
    else
      Right(Username(normalized))
