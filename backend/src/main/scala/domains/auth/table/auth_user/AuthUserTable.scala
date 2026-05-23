package domains.auth.table.auth_user



import cats.effect.IO
import domains.auth.model.{AuthUser, EmailAddress, PasswordHash}
import domains.user.model.{DisplayName, Username}
import domains.problem.model.ProblemTitleDisplayMode
import domains.auth.table.auth_user.AuthUserTableSchema.*
import domains.auth.table.auth_user.AuthUserTableSupport.*
import domains.user.model.{UserDisplayMode, UserLocale}
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.sql.Connection

object AuthUserTable:

  private val logger = Slf4jLogger.getLogger[IO]

  private val seedAdminUsername: Username = Username.canonical("admin")
  private val seedAdminDisplayName: DisplayName = DisplayName("Admin User")
  private val seedAdminEmail: EmailAddress = EmailAddress("admin@example.com")
  private val seedAdminSiteManager: Boolean = true
  private val seedAdminProblemManager: Boolean = true

  private val seedAuthAdminSQL: String =
    """
      |insert into auth_users (username, display_name, email, display_mode, locale, problem_title_display_mode, auto_mark_message_read, password_hash, site_manager, problem_manager)
      |values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      |on conflict (username) do nothing
      |""".stripMargin

  private def seedAdmin(connection: java.sql.Connection, passwordHash: PasswordHash): IO[Unit] =
    for
      _ <- IO.blocking {
        val statement = connection.prepareStatement(seedAuthAdminSQL)
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

  def initialize(connection: Connection, seedAdminPasswordHash: PasswordHash): IO[Unit] =
    for
      _ <- AuthUserTableSchema.initializeSchema(connection)
      _ <- seedAdmin(connection, seedAdminPasswordHash)
    yield ()

  private val findAuthUserByUsernameSQL: String =
    """
      |select username, display_name, email, display_mode, locale, problem_title_display_mode, auto_mark_message_read, password_hash, site_manager, problem_manager
      |from auth_users
      |where lower(username) = lower(?)
      |""".stripMargin

  def findByUsername(connection: Connection, username: Username): IO[Option[AuthUser]] =
    IO.blocking {
      val statement = connection.prepareStatement(findAuthUserByUsernameSQL)
      try
        statement.setString(1, username.value.trim)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAuthUser(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val insertAuthUserSQL: String =
    """
      |insert into auth_users (username, display_name, email, display_mode, locale, problem_title_display_mode, auto_mark_message_read, password_hash, site_manager, problem_manager)
      |values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      |returning username, display_name, email, display_mode, locale, problem_title_display_mode, auto_mark_message_read, password_hash, site_manager, problem_manager
      |""".stripMargin

  def insert(
    connection: Connection,
    username: Username,
    displayName: DisplayName,
    email: EmailAddress,
    displayMode: UserDisplayMode,
    locale: UserLocale,
    problemTitleDisplayMode: ProblemTitleDisplayMode,
    autoMarkMessageRead: Boolean,
    passwordHash: PasswordHash
  ): IO[AuthUser] =
    IO.blocking {
      val statement = connection.prepareStatement(insertAuthUserSQL)
      try
        statement.setString(1, username.value.trim)
        statement.setString(2, displayName.value.trim)
        statement.setString(3, email.value.trim)
        statement.setString(4, encodeUserDisplayModeColumn(displayMode))
        statement.setString(5, encodeUserLocaleColumn(locale))
        statement.setString(6, encodeProblemTitleDisplayModeColumn(problemTitleDisplayMode))
        statement.setBoolean(7, autoMarkMessageRead)
        statement.setString(8, passwordHash.value)
        statement.setBoolean(9, false)
        statement.setBoolean(10, false)

        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then readAuthUser(resultSet)
          else missingInsertResult("user")
        finally resultSet.close()
      finally statement.close()
    }
