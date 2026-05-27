package domains.user.objects.request

import domains.user.objects.*

import domains.user.objects.DisplayName

final case class UpdateOwnProfileRequest(
  displayName: DisplayName
)
