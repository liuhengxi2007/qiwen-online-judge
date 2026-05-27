package domains.auth.objects.request

import domains.auth.objects.*
import domains.user.objects.{DisplayName, Username}

final case class RegisterRequest(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  password: PlaintextPassword
)
