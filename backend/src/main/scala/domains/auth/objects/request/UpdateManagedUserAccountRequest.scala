package domains.auth.objects.request

import domains.auth.objects.{EmailAddress, PlaintextPassword}

final case class UpdateManagedUserAccountRequest(
  email: EmailAddress,
  newPassword: Option[PlaintextPassword]
)
