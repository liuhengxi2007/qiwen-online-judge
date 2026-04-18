package domains.auth.table

import domains.auth.model.{DisplayName, UserDisplayMode, UserIdentity, UserPreferences, Username}

import java.sql.ResultSet

object UserIdentityTableSupport:

  def readUserIdentity(resultSet: ResultSet, prefix: String): UserIdentity =
    UserIdentity(
      username = Username.canonical(resultSet.getString(s"${prefix}_username")),
      displayName = DisplayName(resultSet.getString(s"${prefix}_display_name")),
      preferences =
        UserPreferences(
          displayMode =
            UserDisplayMode
              .fromDatabase(resultSet.getString(s"${prefix}_display_mode"))
              .getOrElse(throw new IllegalStateException(s"Invalid ${prefix}_display_mode."))
        )
    )
