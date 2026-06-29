package domains.auth.objects

import domains.auth.objects.internal.AuthenticatedUser
import domains.user.objects.Username

/** 已确认具备站点管理权限的认证用户包装，供站点管理员 API 使用；对象对齐例外：这是后端授权证明，不序列化为前端 JSON 载荷。 */
final case class SiteManagerUser private (authenticatedUser: AuthenticatedUser):
  def username: Username = authenticatedUser.username
  def siteManager: Boolean = authenticatedUser.siteManager
  def problemManager: Boolean = authenticatedUser.problemManager
  def contestManager: Boolean = authenticatedUser.contestManager

/** 从普通认证用户提升为站点管理员用户的权限检查入口。 */
object SiteManagerUser:
  /** 权限满足时返回包装后的用户，否则返回 None；无数据库副作用。 */
  def from(authenticatedUser: AuthenticatedUser): Option[SiteManagerUser] =
    val permissions =
      AuthPermissionFlags.normalize(
        authenticatedUser.siteManager,
        authenticatedUser.problemManager,
        authenticatedUser.contestManager
      )
    val normalizedUser =
      authenticatedUser.copy(
        siteManager = permissions.siteManager,
        problemManager = permissions.problemManager,
        contestManager = permissions.contestManager
      )
    Option.when(permissions.siteManager)(SiteManagerUser(normalizedUser))
