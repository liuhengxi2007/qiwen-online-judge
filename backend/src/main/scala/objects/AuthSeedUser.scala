package objects

final case class AuthSeedUser(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  password: PlaintextPassword
)
