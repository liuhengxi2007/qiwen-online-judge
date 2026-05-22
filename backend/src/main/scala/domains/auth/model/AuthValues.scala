package domains.auth.model



final case class EmailAddress(value: String)

final case class PlaintextPassword(value: String)

final case class PasswordHash(value: String)
