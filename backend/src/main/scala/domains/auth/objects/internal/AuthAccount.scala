package domains.auth.objects.internal

import domains.auth.objects.{AuthPermissionFlags, EmailAddress, PasswordHash}
import domains.user.objects.Username

/** 认证账号内部模型，由 AuthAccountTable 读写，供登录校验、账号管理和 AuthAccountListItem 响应构造使用。 */
final case class AuthAccount(
  username: Username,
  email: EmailAddress,
  passwordHash: PasswordHash,
  siteManager: Boolean,
  problemManager: Boolean,
  contestManager: Boolean
):
  /** 丢弃邮箱和密码哈希，只保留运行时鉴权所需的用户身份与归一化权限。 */
  def authenticatedUser: AuthenticatedUser =
    val permissions = AuthPermissionFlags.normalize(siteManager, problemManager, contestManager)
    AuthenticatedUser(
      username = username,
      siteManager = permissions.siteManager,
      problemManager = permissions.problemManager,
      contestManager = permissions.contestManager
    )
