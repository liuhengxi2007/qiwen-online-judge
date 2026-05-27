package domains.user.objects.response

import domains.auth.objects.EmailAddress
import domains.user.objects.{DisplayName, UserPreferences, Username}

final case class UserSettingsResponse(
  displayName: DisplayName,
  username: Username,
  email: EmailAddress,
  preferences: UserPreferences,
  siteManager: Boolean,
  problemManager: Boolean
)
