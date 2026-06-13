package domains.auth.objects

import domains.auth.objects.internal.AuthenticatedUser
import domains.user.objects.Username

/** 已确认具备题目管理权限的认证用户包装，构造入口会归一化权限。 */
final case class ProblemManagerUser private (authenticatedUser: AuthenticatedUser):
  def username: Username = authenticatedUser.username
  def siteManager: Boolean = authenticatedUser.siteManager
  def problemManager: Boolean = authenticatedUser.problemManager
  def contestManager: Boolean = authenticatedUser.contestManager

/** 从普通认证用户提升为题目管理员用户的权限检查入口。 */
object ProblemManagerUser:
  /** 权限满足时返回包装后的用户，否则返回 None；无数据库副作用。 */
  def from(authenticatedUser: AuthenticatedUser): Option[ProblemManagerUser] =
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
    Option.when(permissions.problemManager)(ProblemManagerUser(normalizedUser))
