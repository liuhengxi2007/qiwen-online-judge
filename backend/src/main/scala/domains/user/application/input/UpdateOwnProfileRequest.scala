package domains.user.application.input

import domains.user.model.*

import domains.user.model.DisplayName

final case class UpdateOwnProfileRequest(
  displayName: DisplayName
)
