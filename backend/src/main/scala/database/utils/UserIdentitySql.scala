package database.utils

import java.sql.ResultSet



final case class UserIdentityRow(
  username: String,
  displayName: String
)

object UserIdentitySql:

  def selectColumns(usernameColumn: String, alias: String, displayNameTableAlias: String): String =
    s"$usernameColumn, $displayNameTableAlias.display_name as ${alias}_display_name"

  def selectOptionalColumns(usernameColumn: String, alias: String, displayNameTableAlias: String): String =
    selectColumns(usernameColumn, alias, displayNameTableAlias)

  def joinUserProfiles(usernameColumn: String, displayNameTableAlias: String): String =
    s"join user_profiles $displayNameTableAlias on $displayNameTableAlias.username = $usernameColumn"

  def leftJoinUserProfiles(usernameColumn: String, displayNameTableAlias: String): String =
    s"left join user_profiles $displayNameTableAlias on $displayNameTableAlias.username = $usernameColumn"

  def returningColumns(usernameColumn: String, alias: String): String =
    s"$usernameColumn, (select display_name from user_profiles where username = $usernameColumn) as ${alias}_display_name"

  def returningOptionalColumns(usernameColumn: String, alias: String): String =
    returningColumns(usernameColumn, alias)

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

  def readOptionalUserIdentityRow(resultSet: ResultSet, prefix: String): Option[UserIdentityRow] =
    Option(resultSet.getString(s"${prefix}_username")).map { username =>
      UserIdentityRow(
        username = username,
        displayName = resultSet.getString(s"${prefix}_display_name")
      )
    }
