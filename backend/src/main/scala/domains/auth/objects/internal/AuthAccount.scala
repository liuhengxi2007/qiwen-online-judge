package domains.auth.objects.internal

import domains.auth.objects.{AuthPermissionFlags, EmailAddress, PasswordHash}
import domains.user.objects.Username

final case class AuthAccount(
  username: Username,
  email: EmailAddress,
  passwordHash: PasswordHash,
  siteManager: Boolean,
  problemManager: Boolean,
  contestManager: Boolean
):
  def authenticatedUser: AuthenticatedUser =
    val permissions = AuthPermissionFlags.normalize(siteManager, problemManager, contestManager)
    AuthenticatedUser(
      username = username,
      siteManager = permissions.siteManager,
      problemManager = permissions.problemManager,
      contestManager = permissions.contestManager
    )
