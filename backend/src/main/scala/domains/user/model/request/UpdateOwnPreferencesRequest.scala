package domains.user.model.request

import domains.user.model.*

final case class UpdateOwnPreferencesRequest(
  preferences: UserPreferences
)
