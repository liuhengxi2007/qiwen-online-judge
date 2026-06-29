package domains.auth.objects

/** 账号权限标记，site manager 隐含题目和比赛管理权限。 */
final case class AuthPermissionFlags(
  siteManager: Boolean,
  problemManager: Boolean,
  contestManager: Boolean
)

/** 权限标记归一化工具，保证高权限包含低层管理能力。 */
object AuthPermissionFlags:
  /** 归一化权限布尔值；站点管理员会自动拥有 problem/contest manager。 */
  def normalize(siteManager: Boolean, problemManager: Boolean, contestManager: Boolean): AuthPermissionFlags =
    AuthPermissionFlags(
      siteManager = siteManager,
      problemManager = siteManager || problemManager,
      contestManager = siteManager || contestManager
    )
