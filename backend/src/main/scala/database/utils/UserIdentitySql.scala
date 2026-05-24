package database.utils

import java.sql.ResultSet



final case class UserIdentityRow(
  username: String,
  displayName: String
)

object UserIdentitySql:

  def selectColumns(usernameColumn: String, alias: String, displayNameTableAlias: String): String =
    s"$usernameColumn, $displayNameTableAlias.display_name as ${alias}_display_name"

  def joinAuthUsers(usernameColumn: String, displayNameTableAlias: String): String =
    s"join auth_users $displayNameTableAlias on $displayNameTableAlias.username = $usernameColumn"

  def returningColumns(usernameColumn: String, alias: String): String =
    s"$usernameColumn, (select display_name from auth_users where username = $usernameColumn) as ${alias}_display_name"

  def readUserIdentityRow(resultSet: ResultSet): UserIdentityRow =
    UserIdentityRow(
      username = resultSet.getString("username"),
      displayName = resultSet.getString("display_name")
    )

  def readUserIdentityRow(resultSet: ResultSet, prefix: String): UserIdentityRow =
    UserIdentityRow(
      username = resultSet.getString(s"${prefix}_username"),
      displayName = resultSet.getString(s"${prefix}_display_name")
    )
