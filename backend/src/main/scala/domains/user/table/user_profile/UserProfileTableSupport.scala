package domains.user.table.user_profile



import domains.auth.objects.{AuthPermissionFlags, EmailAddress}
import domains.user.objects.{DisplayName, UserIdentity, Username}
import domains.problem.objects.{ProblemSlug, ProblemTitle, ProblemTitleDisplayMode}
import database.utils.LikePatternSql
import database.utils.UserIdentitySql
import domains.user.objects.UserProfileSettings
import domains.user.objects.response.{ManagedUserListItem, UserAcceptedRanklistItem, UserContributionRanklistItem, UserSettingsResponse}
import domains.user.objects.request.UserSearchQuery
import domains.user.objects.{UserAcceptedProblem, UserAvatarUrl, UserContribution, UserDisplayMode, UserLocale}

import java.sql.{PreparedStatement, ResultSet, Timestamp}

/** user_profiles 相关 ResultSet 读取、列编解码和搜索绑定辅助。 */
object UserProfileTableSupport:

  /** 从默认列名读取用户身份摘要。 */
  def readUserIdentity(resultSet: ResultSet): UserIdentity =
    val row = UserIdentitySql.readUserIdentityRow(resultSet)
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )

  /** 从带前缀列名读取用户身份摘要。 */
  def readUserIdentity(resultSet: ResultSet, prefix: String): UserIdentity =
    val row = UserIdentitySql.readUserIdentityRow(resultSet, prefix)
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )

  /** 从当前行读取用户资料设置，数据库枚举列非法时抛出状态异常。 */
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
      autoMarkMessageRead = resultSet.getBoolean("auto_mark_message_read"),
      avatarUrl = readAvatarUrl(resultSet)
    )

  /** 从当前行读取头像对象 key 和内容类型，三项头像字段不完整时视为无头像。 */
  def readAvatarObject(resultSet: ResultSet): Option[(String, String)] =
    val objectKey = Option(resultSet.getString("avatar_object_key")).map(_.trim).filter(_.nonEmpty)
    val contentType = Option(resultSet.getString("avatar_content_type")).map(_.trim).filter(_.nonEmpty)
    val updatedAt = Option(resultSet.getTimestamp("avatar_updated_at")).map(_.toInstant)
    (objectKey, contentType, updatedAt) match
      case (Some(key), Some(mediaType), Some(_)) =>
        Some(key -> mediaType)
      case _ =>
        None

  /** 根据用户名和更新时间生成头像 URL，更新时间作为缓存破坏参数。 */
  def avatarUrlFor(username: Username, updatedAt: Timestamp): UserAvatarUrl =
    UserAvatarUrl(s"/api/users/${username.value}/avatar?v=${updatedAt.toInstant.toEpochMilli}")

  private def readAvatarUrl(resultSet: ResultSet): Option[UserAvatarUrl] =
    Option(resultSet.getTimestamp("avatar_updated_at")).map(timestamp =>
      avatarUrlFor(Username.canonical(resultSet.getString("username")), timestamp)
    )

  /** 从当前行读取管理端用户列表项，并归一化权限。 */
  def readUserListItem(resultSet: ResultSet): ManagedUserListItem =
    val permissions =
      AuthPermissionFlags.normalize(
        resultSet.getBoolean("site_manager"),
        resultSet.getBoolean("problem_manager"),
        resultSet.getBoolean("contest_manager")
      )
    ManagedUserListItem(
      username = Username.canonical(resultSet.getString("username")),
      displayName = DisplayName(resultSet.getString("display_name")),
      email = EmailAddress(resultSet.getString("email")),
      siteManager = permissions.siteManager,
      problemManager = permissions.problemManager,
      contestManager = permissions.contestManager
    )

  /** 从当前行读取用户设置响应所需的资料、邮箱和权限。 */
  def readUserSettingsResponse(resultSet: ResultSet): UserSettingsResponse =
    UserSettingsResponse.fromParts(
      profile = readProfileSettings(resultSet),
      email = EmailAddress(resultSet.getString("email")),
      siteManager = resultSet.getBoolean("site_manager"),
      problemManager = resultSet.getBoolean("problem_manager"),
      contestManager = resultSet.getBoolean("contest_manager")
    )

  /** 从当前行读取贡献排行榜条目。 */
  def readContributionRanklistItem(resultSet: ResultSet): UserContributionRanklistItem =
    UserContributionRanklistItem(
      user = readUserIdentity(resultSet),
      contribution = UserContribution(BigDecimal(resultSet.getBigDecimal("contribution")))
    )

  /** 从当前行读取 AC 题数排行榜条目。 */
  def readAcceptedRanklistItem(resultSet: ResultSet): UserAcceptedRanklistItem =
    UserAcceptedRanklistItem(
      user = readUserIdentity(resultSet),
      acceptedCount = resultSet.getInt("accepted_count")
    )

  /** 从当前行读取用户已通过题目，题目 slug/title 非法时抛出状态异常。 */
  def readAcceptedProblem(resultSet: ResultSet): UserAcceptedProblem =
    UserAcceptedProblem(
      slug = parseColumn("problems.slug", resultSet.getString("slug"), ProblemSlug.parse),
      title = parseColumn("problems.title", resultSet.getString("title"), ProblemTitle.parse),
      acceptedAt = resultSet.getTimestamp("accepted_at").toInstant
    )

  /** 绑定用户搜索 SQL 参数，返回下一个可用参数下标。 */
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

  /** 将用户显示模式编码为数据库列值。 */
  def encodeUserDisplayModeColumn(value: UserDisplayMode): String =
    value match
      case UserDisplayMode.DisplayName => "display_name"
      case UserDisplayMode.Username => "username"
      case UserDisplayMode.DisplayNameWithUsername => "display_name_with_username"

  /** 从数据库列值解码用户显示模式。 */
  def decodeUserDisplayModeColumn(value: String): Option[UserDisplayMode] =
    UserDisplayMode.parse(value).toOption

  /** 将用户语言偏好编码为数据库列值。 */
  def encodeUserLocaleColumn(value: UserLocale): String =
    value match
      case UserLocale.En => "en"
      case UserLocale.ZhCn => "zh-CN"

  /** 从数据库列值解码用户语言偏好。 */
  def decodeUserLocaleColumn(value: String): Option[UserLocale] =
    UserLocale.parse(value).toOption

  /** 将题目标题显示偏好编码为数据库列值。 */
  def encodeProblemTitleDisplayModeColumn(value: ProblemTitleDisplayMode): String =
    value match
      case ProblemTitleDisplayMode.Title => "title"
      case ProblemTitleDisplayMode.Slug => "slug"
      case ProblemTitleDisplayMode.TitleWithSlug => "title_with_slug"

  /** 从数据库列值解码题目标题显示偏好。 */
  def decodeProblemTitleDisplayModeColumn(value: String): Option[ProblemTitleDisplayMode] =
    ProblemTitleDisplayMode.parse(value).toOption

  private def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)
