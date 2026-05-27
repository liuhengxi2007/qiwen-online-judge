package domains.user.objects.request

import domains.user.objects.*

import domains.user.objects.DisplayName

final case class UpdateManagedUserProfileRequest(
  displayName: DisplayName
)
