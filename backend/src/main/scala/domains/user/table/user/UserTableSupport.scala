package domains.user.table.user



import domains.auth.model.{AuthUser, EmailAddress, PasswordHash}
import domains.user.model.{DisplayName, Username}
import domains.problem.model.{ProblemSlug, ProblemTitle, ProblemTitleDisplayMode}
import shared.sql.LikePatternSql
import shared.sql.UserIdentitySql.readUserIdentity
import domains.user.application.output.{AuthUserListItem, UserAcceptedRanklistItem, UserRanklistItem}
import domains.user.application.input.UserSearchQuery
import domains.user.model.{UserAcceptedProblem, UserContribution, UserDisplayMode, UserLocale}

import java.sql.{PreparedStatement, ResultSet}

object UserTableSupport:

  def readAuthUser(resultSet: ResultSet): AuthUser =
    AuthUser(
      username = Username.canonical(resultSet.getString("username")),
      displayName = DisplayName(resultSet.getString("display_name")),
      email = EmailAddress(resultSet.getString("email")),
      displayMode =
        decodeUserDisplayModeColumn(resultSet.getString("display_mode"))
          .getOrElse(throw new IllegalStateException("Invalid auth_users.display_mode.")),
      locale =
        decodeUserLocaleColumn(resultSet.getString("locale"))
          .getOrElse(throw new IllegalStateException("Invalid auth_users.locale.")),
      problemTitleDisplayMode =
        decodeProblemTitleDisplayModeColumn(resultSet.getString("problem_title_display_mode"))
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

  def encodeUserDisplayModeColumn(value: UserDisplayMode): String =
    value match
      case UserDisplayMode.DisplayName => "display_name"
      case UserDisplayMode.Username => "username"
      case UserDisplayMode.DisplayNameWithUsername => "display_name_with_username"

  def decodeUserDisplayModeColumn(value: String): Option[UserDisplayMode] =
    UserDisplayMode.parse(value).toOption

  def encodeUserLocaleColumn(value: UserLocale): String =
    value match
      case UserLocale.En => "en"
      case UserLocale.ZhCn => "zh-CN"

  def decodeUserLocaleColumn(value: String): Option[UserLocale] =
    UserLocale.parse(value).toOption

  def encodeProblemTitleDisplayModeColumn(value: ProblemTitleDisplayMode): String =
    value match
      case ProblemTitleDisplayMode.Title => "title"
      case ProblemTitleDisplayMode.Slug => "slug"
      case ProblemTitleDisplayMode.TitleWithSlug => "title_with_slug"

  def decodeProblemTitleDisplayModeColumn(value: String): Option[ProblemTitleDisplayMode] =
    ProblemTitleDisplayMode.parse(value).toOption

  private def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)
