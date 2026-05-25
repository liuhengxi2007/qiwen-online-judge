package domains.auth.model.request

final case class UpdateUserPermissionsRequest(
  siteManager: Boolean,
  problemManager: Boolean
)
