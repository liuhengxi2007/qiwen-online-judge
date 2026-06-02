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
    Option.when(authenticatedUser.siteManager)(SiteManagerUser(authenticatedUser))
