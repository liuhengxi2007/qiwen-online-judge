package domains.auth.table.auth_user



import domains.user.model.{DisplayName, Username}
import domains.user.model.UserIdentity

import java.sql.ResultSet

object UserIdentityTableSupport:

  def readUserIdentity(resultSet: ResultSet, prefix: String): UserIdentity =
    UserIdentity(
      username = Username.canonical(resultSet.getString(s"${prefix}_username")),
      displayName = DisplayName(resultSet.getString(s"${prefix}_display_name"))
    )
