package domains.auth.objects

import domains.auth.objects.internal.AuthenticatedUser
import domains.user.objects.Username

final case class SiteManagerUser private (authenticatedUser: AuthenticatedUser):
  def username: Username = authenticatedUser.username
  def siteManager: Boolean = authenticatedUser.siteManager
  def problemManager: Boolean = authenticatedUser.problemManager
  def contestManager: Boolean = authenticatedUser.contestManager

object SiteManagerUser:
  def from(authenticatedUser: AuthenticatedUser): Option[SiteManagerUser] =
    val permissions =
      AuthPermissionFlags.normalize(
        authenticatedUser.siteManager,
        authenticatedUser.problemManager,
        authenticatedUser.contestManager
      )
    val normalizedUser =
      authenticatedUser.copy(
        siteManager = permissions.siteManager,
        problemManager = permissions.problemManager,
        contestManager = permissions.contestManager
      )
    Option.when(permissions.siteManager)(SiteManagerUser(normalizedUser))
