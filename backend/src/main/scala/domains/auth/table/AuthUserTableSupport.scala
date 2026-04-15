package domains.auth.table

import cats.effect.IO
import domains.auth.application.PasswordHasher
import domains.auth.model.{AuthSeedUser, AuthUser, DisplayName, EmailAddress, PasswordHash, PlaintextPassword, Username}
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

  def seedAdmin(connection: java.sql.Connection): IO[Unit] =
    for
      passwordHash <- PasswordHasher.hashPassword(seedAdminUser.password)
      _ <- IO.blocking {
        val statement = connection.prepareStatement(AuthUserTableSql.seedAdminSql)
        try
          statement.setString(1, seedAdminUser.username.value)
          statement.setString(2, seedAdminUser.displayName.value)
          statement.setString(3, seedAdminUser.email.value)
          statement.setString(4, passwordHash.value)
          statement.setBoolean(5, seedAdminUser.siteManager)
          statement.setBoolean(6, seedAdminUser.problemManager)
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
      passwordHash = PasswordHash(resultSet.getString("password_hash")),
      siteManager = resultSet.getBoolean("site_manager"),
      problemManager = resultSet.getBoolean("problem_manager")
    )
