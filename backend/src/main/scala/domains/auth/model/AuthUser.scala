package domains.auth.model

final case class AuthUser(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  displayMode: UserDisplayMode,
  locale: UserLocale,
  problemTitleDisplayMode: domains.problem.model.ProblemTitleDisplayMode,
  passwordHash: PasswordHash,
  siteManager: Boolean,
  problemManager: Boolean
)
