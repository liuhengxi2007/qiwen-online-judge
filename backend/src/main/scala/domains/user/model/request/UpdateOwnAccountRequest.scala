package domains.user.model.request


import domains.auth.model.{EmailAddress, PlaintextPassword}

final case class UpdateOwnAccountRequest(
  email: EmailAddress,
  currentPassword: PlaintextPassword,
  newPassword: Option[PlaintextPassword]
)
