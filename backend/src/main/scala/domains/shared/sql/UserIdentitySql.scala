package domains.shared.sql

object UserIdentitySql:

  def selectColumns(usernameColumn: String, alias: String, displayNameTableAlias: String): String =
    s"$usernameColumn, $displayNameTableAlias.display_name as ${alias}_display_name, $displayNameTableAlias.display_mode as ${alias}_display_mode, $displayNameTableAlias.locale as ${alias}_locale, $displayNameTableAlias.problem_title_display_mode as ${alias}_problem_title_display_mode"

  def joinAuthUsers(usernameColumn: String, displayNameTableAlias: String): String =
    s"join auth_users $displayNameTableAlias on $displayNameTableAlias.username = $usernameColumn"

  def returningColumns(usernameColumn: String, alias: String): String =
    s"$usernameColumn, (select display_name from auth_users where username = $usernameColumn) as ${alias}_display_name, (select display_mode from auth_users where username = $usernameColumn) as ${alias}_display_mode, (select locale from auth_users where username = $usernameColumn) as ${alias}_locale, (select problem_title_display_mode from auth_users where username = $usernameColumn) as ${alias}_problem_title_display_mode"
