package domains.auth.objects

final case class ProblemManagerUser private (authUser: AuthUser)

object ProblemManagerUser:
  def from(authUser: AuthUser): Option[ProblemManagerUser] =
    Option.when(authUser.problemManager)(ProblemManagerUser(authUser))
