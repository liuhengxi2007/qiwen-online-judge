package objects

final case class AuthUser(
  username: String,
  displayName: String,
  passwordHash: String
)
