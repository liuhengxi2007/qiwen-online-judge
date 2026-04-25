package domains.auth.table

import domains.auth.application.PasswordHasher
import cats.effect.IO
import domains.auth.model.{
  AuthUser,
  DisplayName,
  EmailAddress,
  PlaintextPassword,
  Username
}
import domains.problem.model.ProblemTitleDisplayMode
import domains.auth.table.AuthUserTableSchema.*
import domains.auth.table.AuthUserTableSql.*
import domains.auth.table.AuthUserTableSupport.*
import domains.user.model.{UserDisplayMode, UserLocale}

import java.sql.Connection

object AuthUserTable:

  def initialize(connection: Connection): IO[Unit] =
    for
      _ <- AuthUserTableSchema.initializeSchema(connection)
      _ <- seedAdmin(connection)
    yield ()

  def findByUsername(connection: Connection, username: Username): IO[Option[AuthUser]] =
    IO.blocking {
      val statement = connection.prepareStatement(findAuthUserByUsernameSql)
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
    locale: UserLocale,
    problemTitleDisplayMode: ProblemTitleDisplayMode,
    password: PlaintextPassword
  ): IO[AuthUser] =
    for
      passwordHash <- PasswordHasher.hashPassword(password)
      user <- IO.blocking {
        val statement = connection.prepareStatement(insertAuthUserSql)
        try
          statement.setString(1, username.value.trim)
          statement.setString(2, displayName.value.trim)
          statement.setString(3, email.value.trim)
          statement.setString(4, UserDisplayMode.toDatabase(displayMode))
          statement.setString(5, UserLocale.toDatabase(locale))
          statement.setString(6, ProblemTitleDisplayMode.toDatabase(problemTitleDisplayMode))
          statement.setString(7, passwordHash.value)
          statement.setBoolean(8, false)
          statement.setBoolean(9, false)

          val resultSet = statement.executeQuery()
          try
            if resultSet.next() then readAuthUser(resultSet)
            else missingInsertResult("user")
          finally resultSet.close()
        finally statement.close()
      }
    yield user
