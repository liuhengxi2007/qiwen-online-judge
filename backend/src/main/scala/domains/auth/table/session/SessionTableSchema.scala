package domains.auth.table.session



import cats.effect.IO
import domains.auth.model.SessionToken

import java.sql.{Connection, Timestamp}
import java.time.Duration

object SessionTableSchema:

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

  private val selectMissingTimestampsSQL: String =
    """
      |select token, created_at, last_active_at, expires_at
      |from auth_sessions
      |where last_active_at is null or expires_at is null
      |""".stripMargin

  private val backfillTimestampsSQL: String =
    """
      |update auth_sessions
      |set last_active_at = ?, expires_at = ?
      |where token = ?
      |""".stripMargin

  private def backfillSessionTimestamps(connection: java.sql.Connection, sessionTtl: Duration): IO[Unit] =
    IO.blocking {
      val selectStatement = connection.prepareStatement(selectMissingTimestampsSQL)
      try
        val resultSet = selectStatement.executeQuery()
        val rows =
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ =>
                val token = SessionToken
                  .parse(resultSet.getString("token"))
                  .fold(message => throw new IllegalStateException(message), identity)
                (
                  token,
                  Option(resultSet.getTimestamp("created_at"))
                    .map(_.toInstant)
                    .getOrElse(throw new IllegalStateException(s"auth_sessions.created_at is missing for token ${token.value}")),
                  Option(resultSet.getTimestamp("last_active_at")).map(_.toInstant),
                  Option(resultSet.getTimestamp("expires_at")).map(_.toInstant),
                )
              )
              .toList
          finally resultSet.close()

        val updateStatement = connection.prepareStatement(backfillTimestampsSQL)
        try
          rows.foreach { (token, createdAt, maybeLastActiveAt, maybeExpiresAt) =>
            val lastActiveAt = maybeLastActiveAt.getOrElse(createdAt)
            val expiresAt = maybeExpiresAt.getOrElse(lastActiveAt.plus(sessionTtl))
            updateStatement.setTimestamp(1, Timestamp.from(lastActiveAt))
            updateStatement.setTimestamp(2, Timestamp.from(expiresAt))
            updateStatement.setString(3, token.value)
            updateStatement.addBatch()
          }
          if rows.nonEmpty then updateStatement.executeBatch()
          ()
        finally updateStatement.close()
      finally selectStatement.close()
    }

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
