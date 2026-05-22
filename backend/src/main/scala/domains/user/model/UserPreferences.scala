package domains.user.model



import domains.problem.model.ProblemTitleDisplayMode

final case class UserPreferences(
  displayMode: UserDisplayMode,
  locale: UserLocale,
  problemTitleDisplayMode: ProblemTitleDisplayMode,
  autoMarkMessageRead: Boolean
)
