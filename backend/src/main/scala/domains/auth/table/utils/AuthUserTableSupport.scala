package domains.auth.table.utils



import domains.auth.table.AuthUserTableSql
import cats.effect.IO
import domains.auth.model.{AuthUser, EmailAddress, PasswordHash, PlaintextPassword}
import domains.user.model.{DisplayName, Username}
import domains.problem.model.ProblemTitleDisplayMode
import domains.user.model.{UserDisplayMode, UserLocale}
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.sql.ResultSet

object AuthUserTableSupport:

  private val logger = Slf4jLogger.getLogger[IO]

  private val seedAdminUsername: Username = Username.canonical("admin")
  private val seedAdminDisplayName: DisplayName = DisplayName("Admin User")
  private val seedAdminEmail: EmailAddress = EmailAddress("admin@example.com")
  val seedAdminPlaintextPassword: PlaintextPassword = PlaintextPassword("password123")
  private val seedAdminSiteManager: Boolean = true
  private val seedAdminProblemManager: Boolean = true

  def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")

  def seedAdmin(connection: java.sql.Connection, passwordHash: PasswordHash): IO[Unit] =
    for
      _ <- IO.blocking {
        val statement = connection.prepareStatement(AuthUserTableSql.seedAuthAdminSql)
        try
          statement.setString(1, seedAdminUsername.value)
          statement.setString(2, seedAdminDisplayName.value)
          statement.setString(3, seedAdminEmail.value)
          statement.setString(4, encodeUserDisplayModeColumn(UserDisplayMode.DisplayName))
          statement.setString(5, encodeUserLocaleColumn(UserLocale.En))
          statement.setString(6, encodeProblemTitleDisplayModeColumn(ProblemTitleDisplayMode.Title))
          statement.setBoolean(7, false)
          statement.setString(8, passwordHash.value)
          statement.setBoolean(9, seedAdminSiteManager)
          statement.setBoolean(10, seedAdminProblemManager)
          statement.executeUpdate()
        finally statement.close()
      }
      _ <- logger.info(s"Ensured seeded auth user exists, username=${seedAdminUsername.value}")
    yield ()

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
