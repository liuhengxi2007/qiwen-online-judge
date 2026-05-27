package domains.auth.objects.request

import domains.auth.objects.*
import domains.user.objects.Username

final case class LoginRequest(username: Username, password: PlaintextPassword)
