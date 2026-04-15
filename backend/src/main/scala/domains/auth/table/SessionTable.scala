package domains.auth.table

import cats.effect.IO
import domains.auth.model.Username
import domains.auth.table.SessionTableSchema.*
import domains.auth.table.SessionTableSql.*
import domains.auth.table.SessionTableSupport.*

import java.sql.Connection
import java.sql.Timestamp
import java.time.{Duration, Instant}

object SessionTable:

  def initialize(connection: Connection, sessionTtl: Duration): IO[Unit] =
    SessionTableSchema.initialize(connection, sessionTtl)

  def insert(connection: Connection, token: String, username: Username, expiresAt: Instant): IO[Unit] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(insertSql)
      try
        statement.setString(1, token)
        statement.setString(2, username.value)
        statement.setTimestamp(3, Timestamp.from(now))
        statement.setTimestamp(4, Timestamp.from(now))
        statement.setTimestamp(5, Timestamp.from(expiresAt))
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  def touchAndFindUsernameByToken(
    connection: Connection,
    token: String,
    activeExtensionThreshold: Duration
  ): IO[Option[Username]] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(findSessionByTokenSql)
      try
        statement.setString(1, token)
        statement.setTimestamp(2, Timestamp.from(now))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            val username = Username.canonical(resultSet.getString("username"))
            val currentExpiresAt = resultSet.getTimestamp("expires_at").toInstant
            val nextExpiresAt = maxInstant(currentExpiresAt, now.plus(activeExtensionThreshold))
            val touchStatement = connection.prepareStatement(touchSessionSql)
            try
              touchStatement.setTimestamp(1, Timestamp.from(now))
              touchStatement.setTimestamp(2, Timestamp.from(nextExpiresAt))
              touchStatement.setString(3, token)
              touchStatement.executeUpdate()
            finally touchStatement.close()
            Some(username)
          else None
        finally resultSet.close()
      finally statement.close()
    }

  def deleteExpired(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteExpiredSql)
      try
        statement.setTimestamp(1, Timestamp.from(Instant.now()))
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  def deleteByToken(connection: Connection, token: String): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteByTokenSql)
      try
        statement.setString(1, token)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  def deleteByUsername(connection: Connection, username: Username): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteByUsernameSql)
      try
        statement.setString(1, username.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }
