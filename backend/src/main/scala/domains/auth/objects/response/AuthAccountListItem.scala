package domains.auth.objects.response

import domains.auth.objects.EmailAddress
import domains.user.objects.{DisplayName, Username}

final case class AuthAccountListItem(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  siteManager: Boolean,
  problemManager: Boolean
)
