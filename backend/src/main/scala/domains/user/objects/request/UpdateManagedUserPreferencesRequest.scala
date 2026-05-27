package domains.user.objects.request

import domains.user.objects.*

final case class UpdateManagedUserPreferencesRequest(
  preferences: UserPreferences
)
