package domains.auth.model



final case class SiteManagerUser private (authUser: AuthUser)

object SiteManagerUser:
  def from(authUser: AuthUser): Option[SiteManagerUser] =
    Option.when(authUser.siteManager)(SiteManagerUser(authUser))

final case class ProblemManagerUser private (authUser: AuthUser)

object ProblemManagerUser:
  def from(authUser: AuthUser): Option[ProblemManagerUser] =
    Option.when(authUser.problemManager)(ProblemManagerUser(authUser))
