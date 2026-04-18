package domains.auth.table

import domains.auth.application.PasswordHasher
import cats.effect.IO
import domains.auth.model.{
  AuthSeedUser,
  AuthUser,
  AuthUserListItem,
  DisplayName,
  EmailAddress,
  PasswordHash,
  PlaintextPassword,
  SiteManagerUser,
  UserDisplayMode,
  Username
}
import domains.auth.table.AuthUserTableSchema.*
import domains.auth.table.AuthUserTableSql.*
import domains.auth.table.AuthUserTableSupport.*
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.sql.{Connection, PreparedStatement, ResultSet, SQLException}

object AuthUserTable:

  private val logger = Slf4jLogger.getLogger[IO]

  enum DeleteUserTableResult:
    case NotFound
    case Deleted
    case HasOwnedResources

  def initialize(connection: Connection): IO[Unit] =
    for
      _ <- AuthUserTableSchema.initializeSchema(connection)
      _ <- seedAdmin(connection)
    yield ()

  def findByUsername(connection: Connection, username: Username): IO[Option[AuthUser]] =
    IO.blocking {
      val statement = connection.prepareStatement(findByUsernameSql)
      try
        statement.setString(1, username.value.trim)
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAuthUser(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  def insert(
    connection: Connection,
    username: Username,
    displayName: DisplayName,
    email: EmailAddress,
    displayMode: UserDisplayMode,
    password: PlaintextPassword
  ): IO[AuthUser] =
    for
      passwordHash <- PasswordHasher.hashPassword(password)
      user <- IO.blocking {
        val statement = connection.prepareStatement(insertSql)
        try
          statement.setString(1, username.value.trim)
          statement.setString(2, displayName.value.trim)
          statement.setString(3, email.value.trim)
          statement.setString(4, UserDisplayMode.toDatabase(displayMode))
          statement.setString(5, passwordHash.value)
          statement.setBoolean(6, false)
          statement.setBoolean(7, false)

          val resultSet = statement.executeQuery()
          try
            if resultSet.next() then readAuthUser(resultSet)
            else missingInsertResult("user")
          finally resultSet.close()
        finally statement.close()
      }
    yield user

  def listUsers(connection: Connection, actor: SiteManagerUser): IO[List[AuthUserListItem]] =
    IO.blocking {
      val _ = actor
      val statement = connection.prepareStatement(listUsersSql)
      try
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ =>
              AuthUserListItem(
                username = Username.canonical(resultSet.getString("username")),
                displayName = DisplayName(resultSet.getString("display_name")),
                email = EmailAddress(resultSet.getString("email")),
                siteManager = resultSet.getBoolean("site_manager"),
                problemManager = resultSet.getBoolean("problem_manager")
              )
            )
            .toList
        finally resultSet.close()
      finally statement.close()
    }

  def delete(connection: Connection, username: Username): IO[DeleteUserTableResult] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteSql)
      try
        statement.setString(1, username.value)
        try
          val deletedRows = statement.executeUpdate()
          if deletedRows == 0 then DeleteUserTableResult.NotFound
          else DeleteUserTableResult.Deleted
        catch
          case exception: SQLException if exception.getSQLState == "23503" =>
            DeleteUserTableResult.HasOwnedResources
      finally statement.close()
    }

  def updatePermissions(
    connection: Connection,
    actor: SiteManagerUser,
    username: Username,
    siteManager: Boolean,
    problemManager: Boolean
  ): IO[Option[AuthUser]] =
    IO.blocking {
      val _ = actor
      val statement = connection.prepareStatement(updatePermissionsSql)
      try
        statement.setBoolean(1, siteManager)
        statement.setBoolean(2, problemManager)
        statement.setString(3, username.value.trim)

        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAuthUser(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  def updateSettings(
    connection: Connection,
    username: Username,
    displayName: DisplayName,
    email: EmailAddress,
    displayMode: UserDisplayMode,
    passwordHash: PasswordHash
  ): IO[Option[AuthUser]] =
    IO.blocking {
      val statement = connection.prepareStatement(updateOwnSettingsSql)
      try
        statement.setString(1, displayName.value.trim)
        statement.setString(2, email.value.trim)
        statement.setString(3, UserDisplayMode.toDatabase(displayMode))
        statement.setString(4, passwordHash.value)
        statement.setString(5, username.value.trim)

        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(readAuthUser(resultSet))
          else None
        finally resultSet.close()
      finally statement.close()
    }
