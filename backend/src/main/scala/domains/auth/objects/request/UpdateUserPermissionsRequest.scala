package domains.auth.objects.request

final case class UpdateUserPermissionsRequest(
  siteManager: Boolean,
  problemManager: Boolean
)
