package domains.auth.objects.request

import domains.auth.objects.{EmailAddress, PlaintextPassword}

final case class UpdateOwnAccountRequest(
  email: EmailAddress,
  currentPassword: PlaintextPassword,
  newPassword: Option[PlaintextPassword]
)
