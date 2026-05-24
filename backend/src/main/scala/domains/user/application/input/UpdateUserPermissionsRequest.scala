package domains.user.application.input


final case class UpdateUserPermissionsRequest(
  siteManager: Boolean,
  problemManager: Boolean
)
