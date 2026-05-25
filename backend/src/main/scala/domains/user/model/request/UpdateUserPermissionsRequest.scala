package domains.user.model.request


final case class UpdateUserPermissionsRequest(
  siteManager: Boolean,
  problemManager: Boolean
)
