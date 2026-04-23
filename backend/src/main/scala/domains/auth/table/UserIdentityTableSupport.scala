package domains.auth.table

import domains.auth.model.{DisplayName, Username}
import domains.user.model.UserIdentity

import java.sql.ResultSet

object UserIdentityTableSupport:

  def readUserIdentity(resultSet: ResultSet, prefix: String): UserIdentity =
    UserIdentity(
      username = Username.canonical(resultSet.getString(s"${prefix}_username")),
      displayName = DisplayName(resultSet.getString(s"${prefix}_display_name"))
    )
