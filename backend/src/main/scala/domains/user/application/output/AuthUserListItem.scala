package domains.user.application.output

import domains.user.model.*

import domains.auth.model.EmailAddress
import domains.user.model.{DisplayName, Username}

final case class AuthUserListItem(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  siteManager: Boolean,
  problemManager: Boolean
)
