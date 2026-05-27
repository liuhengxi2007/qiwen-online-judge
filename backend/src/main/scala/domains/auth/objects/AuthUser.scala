package domains.auth.objects



import domains.user.objects.{DisplayName, UserDisplayMode, UserLocale, Username}

final case class AuthUser(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  displayMode: UserDisplayMode,
  locale: UserLocale,
  problemTitleDisplayMode: domains.problem.objects.ProblemTitleDisplayMode,
  autoMarkMessageRead: Boolean,
  passwordHash: PasswordHash,
  siteManager: Boolean,
  problemManager: Boolean
)
