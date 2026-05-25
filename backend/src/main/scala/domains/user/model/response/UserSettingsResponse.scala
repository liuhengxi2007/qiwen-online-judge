package domains.user.model.response

import domains.auth.model.EmailAddress
import domains.user.model.{DisplayName, UserPreferences, Username}

final case class UserSettingsResponse(
  displayName: DisplayName,
  username: Username,
  email: EmailAddress,
  preferences: UserPreferences,
  siteManager: Boolean,
  problemManager: Boolean
)
