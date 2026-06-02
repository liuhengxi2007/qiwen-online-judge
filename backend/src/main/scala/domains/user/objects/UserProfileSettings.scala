package domains.user.objects

import domains.problem.objects.ProblemTitleDisplayMode

final case class UserProfileSettings(
  username: Username,
  displayName: DisplayName,
  displayMode: UserDisplayMode,
  locale: UserLocale,
  problemTitleDisplayMode: ProblemTitleDisplayMode,
  autoMarkMessageRead: Boolean,
  avatarUrl: Option[UserAvatarUrl]
):
  def preferences: UserPreferences =
    UserPreferences(
      displayMode = displayMode,
      locale = locale,
      problemTitleDisplayMode = problemTitleDisplayMode,
      autoMarkMessageRead = autoMarkMessageRead
    )
