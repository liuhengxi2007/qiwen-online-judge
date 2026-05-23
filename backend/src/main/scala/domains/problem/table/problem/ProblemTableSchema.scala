package domains.problem.table.problem



import cats.effect.IO

import java.sql.Connection

object ProblemTableSchema:

  val initTableSql: String =
    """
      |create table if not exists problems (
      |  id uuid primary key,
      |  slug varchar(64) not null unique,
      |  title varchar(120) not null,
      |  statement_text text not null,
      |  data_name varchar(255),
      |  data_bytes bytea,
      |  ready boolean not null default false,
      |  time_limit_ms integer not null default 1000,
      |  space_limit_mb integer not null default 256,
      |  base_access varchar(32) not null default 'owner_only' check (base_access in ('owner_only', 'public')),
      |  others_submission_access varchar(32) not null default 'none' check (others_submission_access in ('none', 'summary', 'detail')),
      |  creator_username varchar(120) not null references auth_users(username),
      |  created_at timestamp not null,
      |  updated_at timestamp not null
      |);
      |""".stripMargin

  val migrateCreatorUsernameColumnSql: String =
    """
      |do $$
      |begin
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'owner_username'
      |  ) and not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'creator_username'
      |  ) then
      |    alter table problems rename column owner_username to creator_username;
      |  end if;
      |end $$;
      |""".stripMargin

  val addBaseAccessColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'base_access'
      |  ) then
      |    alter table problems add column base_access varchar(32);
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'visibility'
      |  ) then
      |    update problems
      |    set base_access = case visibility
      |      when 'public' then 'public'
      |      else 'owner_only'
      |    end
      |    where base_access is null or btrim(base_access) = '';
      |  else
      |    update problems
      |    set base_access = 'owner_only'
      |    where base_access is null or btrim(base_access) = '';
      |  end if;
      |end $$;
      |""".stripMargin

  val addVisibilityColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'visibility'
      |  ) then
      |    alter table problems add column visibility varchar(32);
      |  end if;
      |
      |  update problems
      |  set visibility = case base_access
      |    when 'public' then 'public'
      |    else 'private'
      |  end
      |  where visibility is null or btrim(visibility) = '';
      |end $$;
      |""".stripMargin

  val addDataAndLimitColumnsSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'ready'
      |  ) then
      |    alter table problems add column ready boolean;
      |  end if;
      |
      |  update problems
      |  set ready = false
      |  where ready is null;
      |
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'data_name'
      |  ) then
      |    alter table problems add column data_name varchar(255);
      |  end if;
      |
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'data_bytes'
      |  ) then
      |    alter table problems add column data_bytes bytea;
      |  end if;
      |
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'time_limit_ms'
      |  ) then
      |    alter table problems add column time_limit_ms integer;
      |  end if;
      |
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'space_limit_mb'
      |  ) then
      |    alter table problems add column space_limit_mb integer;
      |  end if;
      |
      |  update problems
      |  set time_limit_ms = 1000
      |  where time_limit_ms is null;
      |
      |  update problems
      |  set space_limit_mb = 256
      |  where space_limit_mb is null;
      |end $$;
      |""".stripMargin

  val setTimeLimitNotNullSql: String =
    """
      |alter table problems
      |alter column time_limit_ms set not null
      |""".stripMargin

  val setTimeLimitDefaultSql: String =
    """
      |alter table problems
      |alter column time_limit_ms set default 1000
      |""".stripMargin

  val setSpaceLimitNotNullSql: String =
    """
      |alter table problems
      |alter column space_limit_mb set not null
      |""".stripMargin

  val setSpaceLimitDefaultSql: String =
    """
      |alter table problems
      |alter column space_limit_mb set default 256
      |""".stripMargin

  val setReadyNotNullSql: String =
    """
      |alter table problems
      |alter column ready set not null
      |""".stripMargin

  val setReadyDefaultSql: String =
    """
      |alter table problems
      |alter column ready set default false
      |""".stripMargin

  val setBaseAccessNotNullSql: String =
    """
      |alter table problems
      |alter column base_access set not null
      |""".stripMargin

  val setBaseAccessDefaultSql: String =
    """
      |alter table problems
      |alter column base_access set default 'owner_only'
      |""".stripMargin

  val addOthersSubmissionAccessColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'others_submission_access'
      |  ) then
      |    alter table problems add column others_submission_access varchar(32);
      |  end if;
      |
      |  update problems
      |  set others_submission_access = 'none'
      |  where others_submission_access is null or btrim(others_submission_access) = '';
      |end $$;
      |""".stripMargin

  val setOthersSubmissionAccessNotNullSql: String =
    """
      |alter table problems
      |alter column others_submission_access set not null
      |""".stripMargin

  val setOthersSubmissionAccessDefaultSql: String =
    """
      |alter table problems
      |alter column others_submission_access set default 'none'
      |""".stripMargin

  val dropStatusColumnSql: String =
    """
      |alter table problems
      |drop column if exists status
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(initTableSql)
        statement.execute(migrateCreatorUsernameColumnSql)
        statement.execute(addVisibilityColumnSql)
        statement.execute(addBaseAccessColumnSql)
        statement.execute(addDataAndLimitColumnsSql)
        statement.execute(setReadyDefaultSql)
        statement.execute(setReadyNotNullSql)
        statement.execute(setBaseAccessDefaultSql)
        statement.execute(setBaseAccessNotNullSql)
        statement.execute(addOthersSubmissionAccessColumnSql)
        statement.execute(setOthersSubmissionAccessDefaultSql)
        statement.execute(setOthersSubmissionAccessNotNullSql)
        statement.execute(setTimeLimitDefaultSql)
        statement.execute(setTimeLimitNotNullSql)
        statement.execute(setSpaceLimitDefaultSql)
        statement.execute(setSpaceLimitNotNullSql)
        statement.execute(dropStatusColumnSql)
      finally statement.close()
    }
