package domains.auth.objects.response

import domains.auth.objects.*

import domains.user.objects.{DisplayName, UserPreferences, Username}

final case class SessionResponse(
  displayName: DisplayName,
  username: Username,
  email: EmailAddress,
  preferences: UserPreferences,
  siteManager: Boolean,
  problemManager: Boolean
)
