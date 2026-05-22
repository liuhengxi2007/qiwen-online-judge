package domains.user.application.input

import domains.user.model.*

final case class UpdateManagedUserPreferencesRequest(
  preferences: UserPreferences
)
