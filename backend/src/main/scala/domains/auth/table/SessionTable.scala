package domains.auth.table

import cats.effect.IO
import domains.auth.model.Username

import java.sql.Connection
import java.sql.Timestamp
import java.time.{Duration, Instant}

object SessionTable:

  val initTableSql: String =
    """
      |create table if not exists auth_sessions (
      |  token varchar(255) primary key,
      |  username varchar(120) not null references auth_users(username) on delete cascade,
      |  created_at timestamp not null,
      |  expires_at timestamp not null
      |);
      |""".stripMargin

  val addExpiresAtColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_sessions'
      |      and column_name = 'expires_at'
      |  ) then
      |    alter table auth_sessions add column expires_at timestamp;
      |  end if;
      |end $$;
      |""".stripMargin

  val setExpiresAtNotNullSql: String =
    """
      |alter table auth_sessions
      |alter column expires_at set not null
      |""".stripMargin

  val createUsernameIndexSql: String =
    """
      |create index if not exists auth_sessions_username_idx
      |on auth_sessions (lower(username))
      |""".stripMargin

  val createExpiresAtIndexSql: String =
    """
      |create index if not exists auth_sessions_expires_at_idx
      |on auth_sessions (expires_at)
      |""".stripMargin

  val insertSql: String =
    """
      |insert into auth_sessions (token, username, created_at, expires_at)
      |values (?, ?, ?, ?)
      |""".stripMargin

  val findUsernameByTokenSql: String =
    """
      |select username
      |from auth_sessions
      |where token = ?
      |  and expires_at > ?
      |""".stripMargin

  val deleteByTokenSql: String =
    """
      |delete from auth_sessions
      |where token = ?
      |""".stripMargin

  val deleteByUsernameSql: String =
    """
      |delete from auth_sessions
      |where lower(username) = lower(?)
      |""".stripMargin

  val deleteExpiredSql: String =
    """
      |delete from auth_sessions
      |where expires_at <= ?
      |""".stripMargin

  def initialize(connection: Connection, sessionTtl: Duration): IO[Unit] =
    for
      _ <- IO.blocking {
        val statement = connection.createStatement()
        try
          statement.execute(initTableSql)
          statement.execute(addExpiresAtColumnSql)
          statement.execute(createUsernameIndexSql)
          statement.execute(createExpiresAtIndexSql)
        finally statement.close()
      }
      _ <- backfillExpiresAt(connection, sessionTtl)
      _ <- IO.blocking {
        val statement = connection.createStatement()
        try statement.execute(setExpiresAtNotNullSql)
        finally statement.close()
      }
    yield ()

  def insert(connection: Connection, token: String, username: Username, expiresAt: Instant): IO[Unit] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(insertSql)
      try
        statement.setString(1, token)
        statement.setString(2, username.value)
        statement.setTimestamp(3, Timestamp.from(now))
        statement.setTimestamp(4, Timestamp.from(expiresAt))
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  def findUsernameByToken(connection: Connection, token: String): IO[Option[Username]] =
    IO.blocking {
      val statement = connection.prepareStatement(findUsernameByTokenSql)
      try
        statement.setString(1, token)
        statement.setTimestamp(2, Timestamp.from(Instant.now()))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(Username(resultSet.getString("username")))
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

  private def backfillExpiresAt(connection: Connection, sessionTtl: Duration): IO[Unit] =
    IO.blocking {
      val selectStatement = connection.prepareStatement(
        """
          |select token, created_at
          |from auth_sessions
          |where expires_at is null
          |""".stripMargin
      )
      try
        val resultSet = selectStatement.executeQuery()
        val rows =
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ =>
                (
                  resultSet.getString("token"),
                  resultSet.getTimestamp("created_at").toInstant.plus(sessionTtl),
                )
              )
              .toList
          finally resultSet.close()

        val updateStatement = connection.prepareStatement(
          """
            |update auth_sessions
            |set expires_at = ?
            |where token = ?
            |""".stripMargin
        )
        try
          rows.foreach { (token, expiresAt) =>
            updateStatement.setTimestamp(1, Timestamp.from(expiresAt))
            updateStatement.setString(2, token)
            updateStatement.addBatch()
          }
          if rows.nonEmpty then
            updateStatement.executeBatch()
          ()
        finally updateStatement.close()
      finally selectStatement.close()
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
