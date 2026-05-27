package domains.user.objects.request

import domains.user.objects.*

final case class UpdateOwnPreferencesRequest(
  preferences: UserPreferences
)
