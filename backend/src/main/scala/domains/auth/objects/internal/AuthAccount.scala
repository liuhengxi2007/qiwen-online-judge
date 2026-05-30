package domains.auth.objects.internal

import domains.auth.objects.{EmailAddress, PasswordHash}
import domains.user.objects.Username

final case class AuthAccount(
  username: Username,
  email: EmailAddress,
  passwordHash: PasswordHash,
  siteManager: Boolean,
  problemManager: Boolean
):
  def authenticatedUser: AuthenticatedUser =
    AuthenticatedUser(
      username = username,
      siteManager = siteManager,
      problemManager = problemManager
    )
