package domains.user.http

import domains.auth.objects.AuthUser
import domains.user.objects.UserPreferences
import domains.user.objects.response.UserSettingsResponse

object UserApiSupport:

  val ranklistPageSize: Int = 10
  val minSuggestionQueryLength: Int = 1

  def toUserSettingsResponse(user: AuthUser): UserSettingsResponse =
    UserSettingsResponse(
      displayName = user.displayName,
      username = user.username,
      email = user.email,
      preferences = UserPreferences(
        displayMode = user.displayMode,
        locale = user.locale,
        problemTitleDisplayMode = user.problemTitleDisplayMode,
        autoMarkMessageRead = user.autoMarkMessageRead
      ),
      siteManager = user.siteManager,
      problemManager = user.problemManager
    )
