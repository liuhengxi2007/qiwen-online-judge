package domains.auth.table.auth_account



import domains.auth.objects.{AuthPermissionFlags, EmailAddress, PasswordHash, PlaintextPassword}
import domains.auth.objects.internal.{AuthAccount, AuthenticatedUser}
import domains.user.objects.Username

import java.sql.ResultSet

object AuthAccountTableSupport:

  val seedAdminPlaintextPassword: PlaintextPassword = PlaintextPassword("password123")

  def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")

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
