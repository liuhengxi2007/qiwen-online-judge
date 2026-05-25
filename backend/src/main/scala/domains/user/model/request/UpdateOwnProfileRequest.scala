package domains.user.model.request

import domains.user.model.*

import domains.user.model.DisplayName

final case class UpdateOwnProfileRequest(
  displayName: DisplayName
)
