package domains.auth.table



import domains.auth.table.utils.SessionTableSupport
import cats.effect.IO

import java.sql.Connection
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
      _ <- SessionTableSupport.backfillSessionTimestamps(connection, sessionTtl)
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
