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
      |  last_active_at timestamp not null,
      |  expires_at timestamp not null
      |);
      |""".stripMargin

  val addLastActiveAtColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_sessions'
      |      and column_name = 'last_active_at'
      |  ) then
      |    alter table auth_sessions add column last_active_at timestamp;
      |  end if;
      |end $$;
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

  val setLastActiveAtNotNullSql: String =
    """
      |alter table auth_sessions
      |alter column last_active_at set not null
      |""".stripMargin

  val setExpiresAtNotNullSql: String =
    """
      |alter table auth_sessions
      |alter column expires_at set not null
      |""".stripMargin

  val createUsernameIndexSql: String =
    """
      |create index if not exists auth_sessions_username_idx
      |on auth_sessions (username)
      |""".stripMargin

  val createExpiresAtIndexSql: String =
    """
      |create index if not exists auth_sessions_expires_at_idx
      |on auth_sessions (expires_at)
      |""".stripMargin

  val insertSql: String =
    """
      |insert into auth_sessions (token, username, created_at, last_active_at, expires_at)
      |values (?, ?, ?, ?, ?)
      |""".stripMargin

  val findSessionByTokenSql: String =
    """
      |select username, expires_at, last_active_at
      |from auth_sessions
      |where token = ?
      |  and expires_at > ?
      |""".stripMargin

  val touchSessionSql: String =
    """
      |update auth_sessions
      |set last_active_at = ?, expires_at = ?
      |where token = ?
      |""".stripMargin

  val deleteByTokenSql: String =
    """
      |delete from auth_sessions
      |where token = ?
      |""".stripMargin

  val deleteByUsernameSql: String =
    """
      |delete from auth_sessions
      |where username = ?
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
          statement.execute(addLastActiveAtColumnSql)
          statement.execute(addExpiresAtColumnSql)
          statement.execute(createUsernameIndexSql)
          statement.execute(createExpiresAtIndexSql)
        finally statement.close()
      }
      _ <- backfillSessionTimestamps(connection, sessionTtl)
      _ <- IO.blocking {
        val statement = connection.createStatement()
        try statement.execute(setLastActiveAtNotNullSql)
        finally statement.close()
      }
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

  private def backfillSessionTimestamps(connection: Connection, sessionTtl: Duration): IO[Unit] =
    IO.blocking {
      val selectStatement = connection.prepareStatement(
        """
          |select token, created_at, last_active_at, expires_at
          |from auth_sessions
          |where last_active_at is null or expires_at is null
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
                  Option(resultSet.getTimestamp("created_at")).map(_.toInstant).getOrElse(Instant.now()),
                  Option(resultSet.getTimestamp("last_active_at")).map(_.toInstant),
                  Option(resultSet.getTimestamp("expires_at")).map(_.toInstant),
                )
              )
              .toList
          finally resultSet.close()

        val updateStatement = connection.prepareStatement(
          """
            |update auth_sessions
            |set last_active_at = ?, expires_at = ?
            |where token = ?
            |""".stripMargin
        )
        try
          rows.foreach { (token, createdAt, maybeLastActiveAt, maybeExpiresAt) =>
            val lastActiveAt = maybeLastActiveAt.getOrElse(createdAt)
            val expiresAt = maybeExpiresAt.getOrElse(createdAt.plus(sessionTtl))
            updateStatement.setTimestamp(1, Timestamp.from(lastActiveAt))
            updateStatement.setTimestamp(2, Timestamp.from(expiresAt))
            updateStatement.setString(3, token)
            updateStatement.addBatch()
          }
          if rows.nonEmpty then
            updateStatement.executeBatch()
          ()
        finally updateStatement.close()
      finally selectStatement.close()
    }

  private def maxInstant(left: Instant, right: Instant): Instant =
    if left.isAfter(right) then left else right

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
