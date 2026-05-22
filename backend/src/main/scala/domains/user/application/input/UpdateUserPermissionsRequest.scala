package domains.user.application.input

import domains.user.model.*

final case class UpdateUserPermissionsRequest(
  siteManager: Boolean,
  problemManager: Boolean
)
