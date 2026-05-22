package domains.auth.table



import cats.effect.IO
import domains.auth.model.SessionToken
import domains.user.model.Username
import domains.auth.table.SessionTableSchema.*
import domains.auth.table.SessionTableSql.*
import domains.auth.table.utils.SessionTableSupport.*

import java.sql.Connection
import java.sql.Timestamp
import java.time.{Duration, Instant}

object SessionTable:
  final case class ActiveSession(
    username: Username,
    expiresAt: Instant
  )

  def initialize(connection: Connection, sessionTtl: Duration): IO[Unit] =
    SessionTableSchema.initialize(connection, sessionTtl)

  def insert(connection: Connection, token: SessionToken, username: Username, expiresAt: Instant): IO[Unit] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(insertSql)
      try
        statement.setString(1, token.value)
        statement.setString(2, username.value)
        statement.setTimestamp(3, Timestamp.from(now))
        statement.setTimestamp(4, Timestamp.from(now))
        statement.setTimestamp(5, Timestamp.from(expiresAt))
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  def findActiveByToken(
    connection: Connection,
    token: SessionToken,
    now: Instant
  ): IO[Option[ActiveSession]] =
    IO.blocking {
      val statement = connection.prepareStatement(findSessionByTokenSql)
      try
        statement.setString(1, token.value)
        statement.setTimestamp(2, Timestamp.from(now))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then
            Some(
              ActiveSession(
                username = Username.canonical(resultSet.getString("username")),
                expiresAt = resultSet.getTimestamp("expires_at").toInstant
              )
            )
          else None
        finally resultSet.close()
      finally statement.close()
    }

  def renewSession(
    connection: Connection,
    token: SessionToken,
    now: Instant,
    nextExpiresAt: Instant
  ): IO[Boolean] =
    IO.blocking {
      val touchStatement = connection.prepareStatement(touchSessionSql)
      try
        touchStatement.setTimestamp(1, Timestamp.from(now))
        touchStatement.setTimestamp(2, Timestamp.from(nextExpiresAt))
        touchStatement.setString(3, token.value)
        touchStatement.setTimestamp(4, Timestamp.from(now))
        touchStatement.executeUpdate() > 0
      finally touchStatement.close()
    }

  def findTokensByUsername(connection: Connection, username: Username): IO[List[SessionToken]] =
    IO.blocking {
      val statement = connection.prepareStatement(findTokensByUsernameSql)
      try
        statement.setString(1, username.value)
        val resultSet = statement.executeQuery()
        try
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ =>
              SessionToken
                .parse(resultSet.getString("token"))
                .fold(message => throw new IllegalStateException(message), identity)
            )
            .toList
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

  def deleteByToken(connection: Connection, token: SessionToken): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(deleteByTokenSql)
      try
        statement.setString(1, token.value)
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
