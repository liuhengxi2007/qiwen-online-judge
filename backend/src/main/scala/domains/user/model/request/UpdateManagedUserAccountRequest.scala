package domains.user.model.request


import domains.auth.model.{EmailAddress, PlaintextPassword}

final case class UpdateManagedUserAccountRequest(
  email: EmailAddress,
  newPassword: Option[PlaintextPassword]
)
