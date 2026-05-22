package domains.user.application.input

import domains.user.model.*

final case class UpdateOwnPreferencesRequest(
  preferences: UserPreferences
)
