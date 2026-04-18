package domains.auth.model

final case class AuthUser(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  displayMode: UserDisplayMode,
  passwordHash: PasswordHash,
  siteManager: Boolean,
  problemManager: Boolean
)
