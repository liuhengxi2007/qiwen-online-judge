package domains.auth.table.auth_user



import domains.auth.model.{AuthUser, EmailAddress, PasswordHash, PlaintextPassword}
import domains.user.model.{DisplayName, Username}
import domains.problem.model.ProblemTitleDisplayMode
import domains.user.model.{UserDisplayMode, UserLocale}

import java.sql.ResultSet

object AuthUserTableSupport:

  val seedAdminPlaintextPassword: PlaintextPassword = PlaintextPassword("password123")

  def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")

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
