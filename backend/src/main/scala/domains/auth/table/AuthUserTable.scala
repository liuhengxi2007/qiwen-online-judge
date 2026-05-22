package domains.auth.table



import cats.effect.IO
import domains.auth.model.{AuthUser, EmailAddress, PasswordHash}
import domains.user.model.{DisplayName, Username}
import domains.problem.model.ProblemTitleDisplayMode
import domains.auth.table.AuthUserTableSchema.*
import domains.auth.table.AuthUserTableSql.*
import domains.auth.table.utils.AuthUserTableSupport.*
import domains.user.model.{UserDisplayMode, UserLocale}

import java.sql.Connection

object AuthUserTable:

  def initialize(connection: Connection, seedAdminPasswordHash: PasswordHash): IO[Unit] =
    for
      _ <- AuthUserTableSchema.initializeSchema(connection)
      _ <- seedAdmin(connection, seedAdminPasswordHash)
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
    autoMarkMessageRead: Boolean,
    passwordHash: PasswordHash
  ): IO[AuthUser] =
    IO.blocking {
      val statement = connection.prepareStatement(insertAuthUserSql)
      try
        statement.setString(1, username.value.trim)
        statement.setString(2, displayName.value.trim)
        statement.setString(3, email.value.trim)
        statement.setString(4, UserDisplayMode.toDatabase(displayMode))
        statement.setString(5, UserLocale.toDatabase(locale))
        statement.setString(6, ProblemTitleDisplayMode.toDatabase(problemTitleDisplayMode))
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
