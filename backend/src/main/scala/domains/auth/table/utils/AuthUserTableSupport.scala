package domains.auth.table.utils



import domains.auth.table.AuthUserTableSql
import cats.effect.IO
import domains.auth.model.{AuthSeedUser, AuthUser, EmailAddress, PasswordHash, PlaintextPassword}
import domains.user.model.{DisplayName, Username}
import domains.problem.model.ProblemTitleDisplayMode
import domains.user.model.{UserDisplayMode, UserLocale}
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.sql.ResultSet

object AuthUserTableSupport:

  private val logger = Slf4jLogger.getLogger[IO]

  val seedAdminUser: AuthSeedUser =
    AuthSeedUser(
      username = Username.canonical("admin"),
      displayName = DisplayName("Admin User"),
      email = EmailAddress("admin@example.com"),
      password = PlaintextPassword("password123"),
      siteManager = true,
      problemManager = true
    )

  def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")

  def seedAdmin(connection: java.sql.Connection, passwordHash: PasswordHash): IO[Unit] =
    for
      _ <- IO.blocking {
        val statement = connection.prepareStatement(AuthUserTableSql.seedAuthAdminSql)
        try
          statement.setString(1, seedAdminUser.username.value)
          statement.setString(2, seedAdminUser.displayName.value)
          statement.setString(3, seedAdminUser.email.value)
          statement.setString(4, UserDisplayMode.toDatabase(UserDisplayMode.DisplayName))
          statement.setString(5, UserLocale.toDatabase(UserLocale.En))
          statement.setString(6, ProblemTitleDisplayMode.toDatabase(ProblemTitleDisplayMode.Title))
          statement.setBoolean(7, false)
          statement.setString(8, passwordHash.value)
          statement.setBoolean(9, seedAdminUser.siteManager)
          statement.setBoolean(10, seedAdminUser.problemManager)
          statement.executeUpdate()
        finally statement.close()
      }
      _ <- logger.info(s"Ensured seeded auth user exists, username=${seedAdminUser.username.value}")
    yield ()

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
