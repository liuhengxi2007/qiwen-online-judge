package domains.user.objects



import domains.problem.objects.ProblemTitleDisplayMode

final case class UserPreferences(
  displayMode: UserDisplayMode,
  locale: UserLocale,
  problemTitleDisplayMode: ProblemTitleDisplayMode,
  autoMarkMessageRead: Boolean
)
