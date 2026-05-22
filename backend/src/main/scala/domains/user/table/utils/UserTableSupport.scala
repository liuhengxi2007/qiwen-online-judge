package domains.user.table.utils



import domains.auth.model.{AuthUser, DisplayName, EmailAddress, PasswordHash, Username}
import domains.problem.model.{ProblemSlug, ProblemTitle, ProblemTitleDisplayMode}
import domains.shared.sql.LikePatternSql
import domains.user.application.view.{AuthUserListItem, UserAcceptedRanklistItem, UserRanklistItem}
import domains.user.model.{UserAcceptedProblem, UserContribution, UserDisplayMode, UserIdentity, UserLocale, UserSearchQuery}

import java.sql.{PreparedStatement, ResultSet}

object UserTableSupport:

  def readAuthUser(resultSet: ResultSet): AuthUser =
    AuthUser(
      username = Username.canonical(resultSet.getString("username")),
      displayName = DisplayName(resultSet.getString("display_name")),
      email = EmailAddress(resultSet.getString("email")),
      displayMode =
        UserDisplayMode
          .fromDatabase(resultSet.getString("display_mode"))
          .getOrElse(throw new IllegalStateException("Invalid auth_users.display_mode.")),
      locale =
        UserLocale
          .fromDatabase(resultSet.getString("locale"))
          .getOrElse(throw new IllegalStateException("Invalid auth_users.locale.")),
      problemTitleDisplayMode =
        ProblemTitleDisplayMode
          .fromDatabase(resultSet.getString("problem_title_display_mode"))
          .getOrElse(throw new IllegalStateException("Invalid auth_users.problem_title_display_mode.")),
      autoMarkMessageRead = resultSet.getBoolean("auto_mark_message_read"),
      passwordHash = PasswordHash(resultSet.getString("password_hash")),
      siteManager = resultSet.getBoolean("site_manager"),
      problemManager = resultSet.getBoolean("problem_manager")
    )

  def readUserListItem(resultSet: ResultSet): AuthUserListItem =
    AuthUserListItem(
      username = Username.canonical(resultSet.getString("username")),
      displayName = DisplayName(resultSet.getString("display_name")),
      email = EmailAddress(resultSet.getString("email")),
      siteManager = resultSet.getBoolean("site_manager"),
      problemManager = resultSet.getBoolean("problem_manager")
    )

  def readRanklistItem(resultSet: ResultSet): UserRanklistItem =
    UserRanklistItem(
      user = readUserIdentity(resultSet),
      contribution = UserContribution(BigDecimal(resultSet.getBigDecimal("contribution")))
    )

  def readAcceptedRanklistItem(resultSet: ResultSet): UserAcceptedRanklistItem =
    UserAcceptedRanklistItem(
      user = readUserIdentity(resultSet),
      acceptedCount = resultSet.getInt("accepted_count")
    )

  def readUserIdentity(resultSet: ResultSet): UserIdentity =
    UserIdentity(
      username = Username.canonical(resultSet.getString("username")),
      displayName = DisplayName(resultSet.getString("display_name"))
    )

  def readAcceptedProblem(resultSet: ResultSet): UserAcceptedProblem =
    UserAcceptedProblem(
      slug = parseColumn("problems.slug", resultSet.getString("slug"), ProblemSlug.parse),
      title = parseColumn("problems.title", resultSet.getString("title"), ProblemTitle.parse),
      acceptedAt = resultSet.getTimestamp("accepted_at").toInstant
    )

  def bindUserSearchQuery(
    statement: PreparedStatement,
    query: Option[UserSearchQuery],
    startIndex: Int
  ): Int =
    val likeQuery = query.map(searchQuery => LikePatternSql.fromRaw(searchQuery.value))
    statement.setBoolean(startIndex, query.nonEmpty)
    statement.setString(startIndex + 1, likeQuery.map(_.containsPattern).getOrElse(""))
    statement.setString(startIndex + 2, likeQuery.map(_.containsPattern).getOrElse(""))
    startIndex + 3

  private def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)
