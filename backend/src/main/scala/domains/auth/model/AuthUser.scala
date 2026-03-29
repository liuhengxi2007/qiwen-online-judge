package objects

final case class AuthUser(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  passwordHash: PasswordHash,
  siteManager: Boolean,
  problemManager: Boolean
)
