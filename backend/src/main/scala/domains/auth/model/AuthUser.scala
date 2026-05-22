package domains.auth.model



import domains.user.model.{DisplayName, UserDisplayMode, UserLocale, Username}

final case class AuthUser(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  displayMode: UserDisplayMode,
  locale: UserLocale,
  problemTitleDisplayMode: domains.problem.model.ProblemTitleDisplayMode,
  autoMarkMessageRead: Boolean,
  passwordHash: PasswordHash,
  siteManager: Boolean,
  problemManager: Boolean
)
