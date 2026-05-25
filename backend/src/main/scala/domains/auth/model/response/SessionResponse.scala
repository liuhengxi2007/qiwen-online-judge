package domains.auth.model.response

import domains.auth.model.*

import domains.user.model.{DisplayName, UserPreferences, Username}

final case class SessionResponse(
  displayName: DisplayName,
  username: Username,
  email: EmailAddress,
  preferences: UserPreferences,
  siteManager: Boolean,
  problemManager: Boolean
)
