package domains.auth.table.auth_account



import domains.auth.objects.{EmailAddress, PasswordHash, PlaintextPassword}
import domains.auth.objects.internal.{AuthAccount, AuthenticatedUser}
import domains.user.objects.Username

import java.sql.ResultSet

object AuthAccountTableSupport:

  val seedAdminPlaintextPassword: PlaintextPassword = PlaintextPassword("password123")

  def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")

  def readAuthAccount(resultSet: ResultSet): AuthAccount =
    AuthAccount(
      username = Username.canonical(resultSet.getString("username")),
      email = EmailAddress(resultSet.getString("email")),
      passwordHash = PasswordHash(resultSet.getString("password_hash")),
      siteManager = resultSet.getBoolean("site_manager"),
      problemManager = resultSet.getBoolean("problem_manager")
    )

  def readAuthenticatedUser(resultSet: ResultSet): AuthenticatedUser =
    AuthenticatedUser(
      username = Username.canonical(resultSet.getString("username")),
      siteManager = resultSet.getBoolean("site_manager"),
      problemManager = resultSet.getBoolean("problem_manager")
    )
