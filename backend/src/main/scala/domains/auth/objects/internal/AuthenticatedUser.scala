package domains.auth.objects.internal

import domains.user.objects.Username

final case class AuthenticatedUser(
  username: Username,
  siteManager: Boolean,
  problemManager: Boolean,
  contestManager: Boolean
)
