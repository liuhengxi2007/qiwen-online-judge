package domains.auth.application

import domains.auth.model.{AuthUser, SessionToken}

object AuthCommandResults:

  enum LoginResult:
    case InvalidCredentials
    case LoggedIn(user: AuthUser, sessionToken: SessionToken)

  enum RegisterResult:
    case ValidationFailed(message: String)
    case UsernameConflict
    case UsernameConflictsWithUserGroup
    case Registered(user: AuthUser, sessionToken: SessionToken)
