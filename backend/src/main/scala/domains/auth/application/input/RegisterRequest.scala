package domains.auth.application.input

import domains.auth.model.*
import domains.user.model.{DisplayName, Username}

final case class RegisterRequest(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  password: PlaintextPassword
)
