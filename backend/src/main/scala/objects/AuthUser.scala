package objects

final case class AuthUser(
  username: String,
  displayName: String,
  email: String,
  passwordHash: String
)
