package domains.user.application.input


import domains.auth.model.{EmailAddress, PlaintextPassword}

final case class UpdateManagedUserAccountRequest(
  email: EmailAddress,
  newPassword: Option[PlaintextPassword]
)
