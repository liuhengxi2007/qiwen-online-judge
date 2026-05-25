package domains.auth.model.response

import domains.auth.model.EmailAddress
import domains.user.model.{DisplayName, Username}

final case class AuthAccountListItem(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  siteManager: Boolean,
  problemManager: Boolean
)
