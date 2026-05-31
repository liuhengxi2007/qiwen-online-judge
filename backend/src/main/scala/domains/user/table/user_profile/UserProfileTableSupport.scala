package domains.user.table.user_profile



import domains.auth.objects.EmailAddress
import domains.user.objects.{DisplayName, UserIdentity, Username}
import domains.problem.objects.{ProblemSlug, ProblemTitle, ProblemTitleDisplayMode}
import database.utils.LikePatternSql
import database.utils.UserIdentitySql
import domains.user.objects.internal.UserProfileSettings
import domains.user.objects.response.{ManagedUserListItem, UserAcceptedRanklistItem, UserContributionRanklistItem, UserSettingsResponse}
import domains.user.objects.request.UserSearchQuery
import domains.user.objects.{UserAcceptedProblem, UserContribution, UserDisplayMode, UserLocale}

import java.sql.{PreparedStatement, ResultSet}

object UserProfileTableSupport:

  def readUserIdentity(resultSet: ResultSet): UserIdentity =
    val row = UserIdentitySql.readUserIdentityRow(resultSet)
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )

  def readUserIdentity(resultSet: ResultSet, prefix: String): UserIdentity =
    val row = UserIdentitySql.readUserIdentityRow(resultSet, prefix)
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )

  def readProfileSettings(resultSet: ResultSet): UserProfileSettings =
    UserProfileSettings(
      username = Username.canonical(resultSet.getString("username")),
      displayName = DisplayName(resultSet.getString("display_name")),
      displayMode =
        decodeUserDisplayModeColumn(resultSet.getString("display_mode"))
          .getOrElse(throw new IllegalStateException("Invalid user_profiles.display_mode.")),
      locale =
        decodeUserLocaleColumn(resultSet.getString("locale"))
          .getOrElse(throw new IllegalStateException("Invalid user_profiles.locale.")),
      problemTitleDisplayMode =
        decodeProblemTitleDisplayModeColumn(resultSet.getString("problem_title_display_mode"))
          .getOrElse(throw new IllegalStateException("Invalid user_profiles.problem_title_display_mode.")),
      autoMarkMessageRead = resultSet.getBoolean("auto_mark_message_read")
    )

  def readUserListItem(resultSet: ResultSet): ManagedUserListItem =
    ManagedUserListItem(
      username = Username.canonical(resultSet.getString("username")),
      displayName = DisplayName(resultSet.getString("display_name")),
      email = EmailAddress(resultSet.getString("email")),
      siteManager = resultSet.getBoolean("site_manager"),
      problemManager = resultSet.getBoolean("problem_manager")
    )

  def readUserSettingsResponse(resultSet: ResultSet): UserSettingsResponse =
    UserSettingsResponse.fromParts(
      profile = readProfileSettings(resultSet),
      email = EmailAddress(resultSet.getString("email")),
      siteManager = resultSet.getBoolean("site_manager"),
      problemManager = resultSet.getBoolean("problem_manager")
    )

  def readContributionRanklistItem(resultSet: ResultSet): UserContributionRanklistItem =
    UserContributionRanklistItem(
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
