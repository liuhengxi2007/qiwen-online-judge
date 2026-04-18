package domains.auth.table

import domains.auth.model.{DisplayName, UserDisplayMode, UserIdentity, UserLocale, UserPreferences, Username}
import domains.problem.model.ProblemTitleDisplayMode

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
              .getOrElse(throw new IllegalStateException(s"Invalid ${prefix}_display_mode.")),
          locale =
            UserLocale
              .fromDatabase(resultSet.getString(s"${prefix}_locale"))
              .getOrElse(throw new IllegalStateException(s"Invalid ${prefix}_locale.")),
          problemTitleDisplayMode =
            ProblemTitleDisplayMode
              .fromDatabase(resultSet.getString(s"${prefix}_problem_title_display_mode"))
              .getOrElse(throw new IllegalStateException(s"Invalid ${prefix}_problem_title_display_mode."))
        )
    )
