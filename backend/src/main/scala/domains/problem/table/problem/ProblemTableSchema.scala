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
      |  base_access varchar(32) not null default 'restricted' check (base_access in ('restricted', 'public')),
      |  other_user_submission_access varchar(32) not null default 'none' check (other_user_submission_access in ('none', 'summary', 'detail')),
      |  creator_username varchar(120) not null references auth_accounts(username),
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
      |    from pg_constraint
      |    where conname = 'problems_base_access_check'
      |      and conrelid = 'problems'::regclass
      |  ) then
      |    alter table problems drop constraint problems_base_access_check;
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
      |    set base_access = case
      |      when base_access = 'public' then 'public'
      |      when base_access = 'restricted' then 'restricted'
      |      when visibility = 'public' then 'public'
      |      else 'restricted'
      |    end
      |    where base_access is null
      |      or btrim(base_access) = ''
      |      or base_access not in ('restricted', 'public');
      |  else
      |    update problems
      |    set base_access = 'restricted'
      |    where base_access is null
      |      or btrim(base_access) = ''
      |      or base_access not in ('restricted', 'public');
      |  end if;
      |
      |  if not exists (
      |    select 1
      |    from pg_constraint
      |    where conname = 'problems_base_access_check'
      |      and conrelid = 'problems'::regclass
      |  ) then
      |    alter table problems add constraint problems_base_access_check check (base_access in ('restricted', 'public'));
      |  end if;
      |end $$;
      |""".stripMargin

  val dropVisibilityColumnSql: String =
    """
      |alter table problems
      |drop column if exists visibility
      |""".stripMargin

  val addDataColumnsSql: String =
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
      |end $$;
      |""".stripMargin

  val dropTimeLimitColumnSql: String =
    """
      |alter table problems
      |drop column if exists time_limit_ms
      |""".stripMargin

  val dropSpaceLimitColumnSql: String =
    """
      |alter table problems
      |drop column if exists space_limit_mb
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
      |alter column base_access set default 'restricted'
      |""".stripMargin

  val migrateOtherUserSubmissionAccessColumnSql: String =
    """
      |do $$
      |begin
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'others_submission_access'
      |  ) and not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'other_user_submission_access'
      |  ) then
      |    alter table problems rename column others_submission_access to other_user_submission_access;
      |  end if;
      |end $$;
      |""".stripMargin

  val addOtherUserSubmissionAccessColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'other_user_submission_access'
      |  ) then
      |    alter table problems add column other_user_submission_access varchar(32) check (other_user_submission_access in ('none', 'summary', 'detail'));
      |  end if;
      |
      |  update problems
      |  set other_user_submission_access = 'none'
      |  where other_user_submission_access is null or btrim(other_user_submission_access) = '';
      |end $$;
      |""".stripMargin

  val setOtherUserSubmissionAccessNotNullSql: String =
    """
      |alter table problems
      |alter column other_user_submission_access set not null
      |""".stripMargin

  val setOtherUserSubmissionAccessDefaultSql: String =
    """
      |alter table problems
      |alter column other_user_submission_access set default 'none'
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
        statement.execute(addBaseAccessColumnSql)
        statement.execute(dropVisibilityColumnSql)
        statement.execute(addDataColumnsSql)
        statement.execute(dropTimeLimitColumnSql)
        statement.execute(dropSpaceLimitColumnSql)
        statement.execute(setReadyDefaultSql)
        statement.execute(setReadyNotNullSql)
        statement.execute(setBaseAccessDefaultSql)
        statement.execute(setBaseAccessNotNullSql)
        statement.execute(migrateOtherUserSubmissionAccessColumnSql)
        statement.execute(addOtherUserSubmissionAccessColumnSql)
        statement.execute(setOtherUserSubmissionAccessDefaultSql)
        statement.execute(setOtherUserSubmissionAccessNotNullSql)
        statement.execute(dropStatusColumnSql)
      finally statement.close()
    }
