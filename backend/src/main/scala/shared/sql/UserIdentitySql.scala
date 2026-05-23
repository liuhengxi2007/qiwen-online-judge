package shared.sql

import domains.user.model.{DisplayName, UserIdentity, Username}

import java.sql.ResultSet



object UserIdentitySql:

  def selectColumns(usernameColumn: String, alias: String, displayNameTableAlias: String): String =
    s"$usernameColumn, $displayNameTableAlias.display_name as ${alias}_display_name"

  def joinAuthUsers(usernameColumn: String, displayNameTableAlias: String): String =
    s"join auth_users $displayNameTableAlias on $displayNameTableAlias.username = $usernameColumn"

  def returningColumns(usernameColumn: String, alias: String): String =
    s"$usernameColumn, (select display_name from auth_users where username = $usernameColumn) as ${alias}_display_name"

  def readUserIdentity(resultSet: ResultSet, prefix: String): UserIdentity =
    UserIdentity(
      username = Username.canonical(resultSet.getString(s"${prefix}_username")),
      displayName = DisplayName(resultSet.getString(s"${prefix}_display_name"))
    )
