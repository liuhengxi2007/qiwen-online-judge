package domains.auth.objects

import domains.auth.objects.internal.AuthenticatedUser
import domains.user.objects.Username

final case class ProblemManagerUser private (authenticatedUser: AuthenticatedUser):
  def username: Username = authenticatedUser.username
  def siteManager: Boolean = authenticatedUser.siteManager
  def problemManager: Boolean = authenticatedUser.problemManager
  def contestManager: Boolean = authenticatedUser.contestManager

object ProblemManagerUser:
  def from(authenticatedUser: AuthenticatedUser): Option[ProblemManagerUser] =
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
    Option.when(permissions.problemManager)(ProblemManagerUser(normalizedUser))
