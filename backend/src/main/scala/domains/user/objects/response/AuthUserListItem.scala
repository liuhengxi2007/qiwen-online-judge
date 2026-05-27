package domains.user.objects.response

import domains.user.objects.*

import domains.auth.objects.EmailAddress
import domains.user.objects.{DisplayName, Username}

final case class AuthUserListItem(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  siteManager: Boolean,
  problemManager: Boolean
)
