package domains.auth.table.auth_account



import cats.effect.IO
import domains.auth.objects.{EmailAddress, PasswordHash, SiteManagerUser}
import domains.auth.objects.internal.{AuthAccount, AuthenticatedUser}
import domains.user.objects.Username
import domains.auth.table.auth_account.AuthAccountTableSchema.*
import domains.auth.table.auth_account.AuthAccountTableSupport.*
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.sql.{Connection, SQLException}

object AuthAccountTable:

  enum DeleteAccountTableResult:
    case NotFound
    case Deleted
    case HasOwnedResources

  private val logger = Slf4jLogger.getLogger[IO]

  private val seedAdminUsername: Username = Username.canonical("admin")
  private val seedAdminEmail: EmailAddress = EmailAddress("admin@example.com")
  private val seedAdminSiteManager: Boolean = true
  private val seedAdminProblemManager: Boolean = true

  private val seedAuthAdminSQL: String =
    """
      |insert into auth_accounts (username, email, password_hash, site_manager, problem_manager)
      |values (?, ?, ?, ?, ?)
      |on conflict (username) do nothing
      |""".stripMargin

  private def seedAdmin(connection: java.sql.Connection, passwordHash: PasswordHash): IO[Unit] =
    for
      _ <- IO.blocking {
        val statement = connection.prepareStatement(seedAuthAdminSQL)
        try
          statement.setString(1, seedAdminUsername.value)
          statement.setString(2, seedAdminEmail.value)
          statement.setString(3, passwordHash.value)
          statement.setBoolean(4, seedAdminSiteManager)
          statement.setBoolean(5, seedAdminProblemManager)
          statement.executeUpdate()
        finally statement.close()
      }
      _ <- logger.info(s"Ensured seeded auth user exists, username=${seedAdminUsername.value}")
    yield ()

  def initialize(connection: Connection, seedAdminPasswordHash: PasswordHash): IO[Unit] =
    for
      _ <- AuthAccountTableSchema.initializeSchema(connection)
      _ <- seedAdmin(connection, seedAdminPasswordHash)
    yield ()

  private val findAuthAccountByUsernameSQL: String =
    """
      |select username, email, password_hash, site_manager, problem_manager
      |from auth_accounts
      |where lower(username) = lower(?)
      |""".stripMargin

  def findAccountByUsername(connection: Connection, username: Username): IO[Option[AuthAccount]] =
    IO.blocking {
      val statement = connection.prepareStatement(findAuthAccountByUsernameSQL)
      try
        statement.setString(1, username.value.trim)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAuthAccount(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val findAuthenticatedUserByUsernameSQL: String =
    """
      |select username, site_manager, problem_manager
      |from auth_accounts
      |where lower(username) = lower(?)
      |""".stripMargin

  def findAuthenticatedUserByUsername(connection: Connection, username: Username): IO[Option[AuthenticatedUser]] =
    IO.blocking {
      val statement = connection.prepareStatement(findAuthenticatedUserByUsernameSQL)
      try
        statement.setString(1, username.value.trim)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAuthenticatedUser(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val insertAuthAccountSQL: String =
    """
      |insert into auth_accounts (username, email, password_hash, site_manager, problem_manager)
      |values (?, ?, ?, ?, ?)
      |returning username, email, password_hash, site_manager, problem_manager
      |""".stripMargin

  def insertAccount(
    connection: Connection,
    username: Username,
    email: EmailAddress,
    passwordHash: PasswordHash
  ): IO[AuthAccount] =
    IO.blocking {
      val statement = connection.prepareStatement(insertAuthAccountSQL)
      try
        statement.setString(1, username.value.trim)
        statement.setString(2, email.value.trim)
        statement.setString(3, passwordHash.value)
        statement.setBoolean(4, false)
        statement.setBoolean(5, false)

        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then readAuthAccount(resultSet)
          else missingInsertResult("user")
        finally resultSet.close()
      finally statement.close()
    }

  private val updateAccountSQL: String =
    """
      |update auth_accounts
      |set email = ?, password_hash = ?
      |where username = ?
      |returning username, email, password_hash, site_manager, problem_manager
      |""".stripMargin

  def updateAccount(
    connection: Connection,
    username: Username,
    email: EmailAddress,
    passwordHash: PasswordHash
  ): IO[Option[AuthAccount]] =
    IO.blocking {
      val statement = connection.prepareStatement(updateAccountSQL)
      try
        statement.setString(1, email.value.trim)
        statement.setString(2, passwordHash.value)
        statement.setString(3, username.value.trim)

        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAuthAccount(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val updatePermissionsSQL: String =
    """
      |update auth_accounts
      |set site_manager = ?, problem_manager = ?
      |where username = ?
      |returning username, email, password_hash, site_manager, problem_manager
      |""".stripMargin

  def updatePermissions(
    connection: Connection,
    actor: SiteManagerUser,
    username: Username,
    siteManager: Boolean,
    problemManager: Boolean
  ): IO[Option[AuthAccount]] =
    IO.blocking {
      val _ = actor
      val statement = connection.prepareStatement(updatePermissionsSQL)
      try
        statement.setBoolean(1, siteManager)
        statement.setBoolean(2, problemManager)
        statement.setString(3, username.value.trim)

        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAuthAccount(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  private val deleteSQL: String =
    """
      |delete from auth_accounts
      |where username = ?
      |""".stripMargin

  def delete(connection: Connection, username: Username): IO[DeleteAccountTableResult] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteSQL)
      try
        statement.setString(1, username.value)
        try
          val deletedRows = statement.executeUpdate()
          if deletedRows == 0 then DeleteAccountTableResult.NotFound
          else DeleteAccountTableResult.Deleted
        catch
          case exception: SQLException if exception.getSQLState == "23503" =>
            DeleteAccountTableResult.HasOwnedResources
      finally statement.close()
    }
