package domains.auth.objects

final case class AuthPermissionFlags(
  siteManager: Boolean,
  problemManager: Boolean,
  contestManager: Boolean
)

object AuthPermissionFlags:
  def normalize(siteManager: Boolean, problemManager: Boolean, contestManager: Boolean): AuthPermissionFlags =
    AuthPermissionFlags(
      siteManager = siteManager,
      problemManager = siteManager || problemManager,
      contestManager = siteManager || contestManager
    )
