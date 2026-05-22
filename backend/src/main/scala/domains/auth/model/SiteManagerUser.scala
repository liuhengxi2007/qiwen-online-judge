package domains.auth.model

final case class SiteManagerUser private (authUser: AuthUser)

object SiteManagerUser:
  def from(authUser: AuthUser): Option[SiteManagerUser] =
    Option.when(authUser.siteManager)(SiteManagerUser(authUser))
