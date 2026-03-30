package domains.auth.model

final case class AuthSeedUser(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  password: PlaintextPassword,
  siteManager: Boolean,
  problemManager: Boolean
)
