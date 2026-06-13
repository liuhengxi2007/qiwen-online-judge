package domains.auth.table.auth_account



import domains.auth.objects.{AuthPermissionFlags, EmailAddress, PasswordHash, PlaintextPassword}
import domains.auth.objects.internal.{AuthAccount, AuthenticatedUser}
import domains.user.objects.Username

import java.sql.ResultSet

/** auth_accounts 表的 ResultSet 读取和表层异常辅助。 */
object AuthAccountTableSupport:

  val seedAdminPlaintextPassword: PlaintextPassword = PlaintextPassword("password123")

  /** 表示 insert returning 没有返回行的异常边界，说明数据库状态不符合预期。 */
  def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")

  /** 从当前 ResultSet 行读取完整账号，并归一化权限标记。 */
  def readAuthAccount(resultSet: ResultSet): AuthAccount =
    val permissions =
      AuthPermissionFlags.normalize(
        resultSet.getBoolean("site_manager"),
        resultSet.getBoolean("problem_manager"),
        resultSet.getBoolean("contest_manager")
      )
    AuthAccount(
      username = Username.canonical(resultSet.getString("username")),
      email = EmailAddress(resultSet.getString("email")),
      passwordHash = PasswordHash(resultSet.getString("password_hash")),
      siteManager = permissions.siteManager,
      problemManager = permissions.problemManager,
      contestManager = permissions.contestManager
    )

  /** 从当前 ResultSet 行读取认证用户身份，并归一化权限标记。 */
  def readAuthenticatedUser(resultSet: ResultSet): AuthenticatedUser =
    val permissions =
      AuthPermissionFlags.normalize(
        resultSet.getBoolean("site_manager"),
        resultSet.getBoolean("problem_manager"),
        resultSet.getBoolean("contest_manager")
      )
    AuthenticatedUser(
      username = Username.canonical(resultSet.getString("username")),
      siteManager = permissions.siteManager,
      problemManager = permissions.problemManager,
      contestManager = permissions.contestManager
    )
