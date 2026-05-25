package domains.user.model.request

import domains.user.model.*

final case class UpdateManagedUserPreferencesRequest(
  preferences: UserPreferences
)
