package domains.user.application.input

import domains.user.model.*

import domains.auth.model.{EmailAddress, PlaintextPassword}

final case class UpdateOwnAccountRequest(
  email: EmailAddress,
  currentPassword: PlaintextPassword,
  newPassword: Option[PlaintextPassword]
)
