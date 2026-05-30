package domains.user.objects.internal

import domains.problem.objects.ProblemTitleDisplayMode
import domains.user.objects.{DisplayName, UserDisplayMode, UserLocale, UserPreferences, Username}

final case class UserProfileSettings(
  username: Username,
  displayName: DisplayName,
  displayMode: UserDisplayMode,
  locale: UserLocale,
  problemTitleDisplayMode: ProblemTitleDisplayMode,
  autoMarkMessageRead: Boolean
):
  def preferences: UserPreferences =
    UserPreferences(
      displayMode = displayMode,
      locale = locale,
      problemTitleDisplayMode = problemTitleDisplayMode,
      autoMarkMessageRead = autoMarkMessageRead
    )
