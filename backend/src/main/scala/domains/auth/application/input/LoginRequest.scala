package domains.auth.application.input

import domains.auth.model.*
import domains.user.model.Username

final case class LoginRequest(username: Username, password: PlaintextPassword)
