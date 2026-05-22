package domains.user.model

import java.util.Locale

final case class Username(value: String)

object Username:
  def normalize(raw: String): String =
    raw.trim.toLowerCase(Locale.ROOT)

  def canonical(raw: String): Username =
    new Username(normalize(raw))
