package shared.model.access

import java.util.Locale

final case class AccessUsername(value: String)

object AccessUsername:
  private val usernamePattern = "^[a-z0-9_-]+$".r

  def normalize(raw: String): String =
    raw.trim.toLowerCase(Locale.ROOT)

  def canonical(raw: String): AccessUsername =
    AccessUsername(normalize(raw))

  def parse(raw: String): Either[String, AccessUsername] =
    val normalized = normalize(raw)

    if normalized.length < 3 || normalized.length > 32 then
      Left("Username must be between 3 and 32 characters.")
    else if !usernamePattern.matches(normalized) then
      Left("Username may contain only lowercase letters, numbers, underscores, and hyphens.")
    else
      Right(AccessUsername(normalized))
