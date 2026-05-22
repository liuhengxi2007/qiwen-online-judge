package domains.auth.model



import domains.user.model.{DisplayName, Username}

final case class AuthSeedUser(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  password: PlaintextPassword,
  siteManager: Boolean,
  problemManager: Boolean
)
