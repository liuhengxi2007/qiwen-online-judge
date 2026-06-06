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
      |  result_display_mode varchar(32) not null default 'score' check (result_display_mode in ('verdict', 'score')),
      |  base_access varchar(32) not null default 'restricted' check (base_access in ('restricted', 'public')),
      |  other_user_submission_access varchar(32) not null default 'none' check (other_user_submission_access in ('none', 'summary', 'detail')),
      |  author_username varchar(120) references auth_accounts(username) on delete set null,
      |  created_at timestamp not null,
      |  updated_at timestamp not null
      |);
      |""".stripMargin

  val migrateAuthorUsernameColumnSql: String =
    """
      |do $$
      |declare
      |  constraint_record record;
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'author_username'
      |  ) then
      |    if exists (
      |      select 1
      |      from information_schema.columns
      |      where table_schema = 'public'
      |        and table_name = 'problems'
      |        and column_name = 'creator_username'
      |    ) then
      |      alter table problems rename column creator_username to author_username;
      |    elsif exists (
      |      select 1
      |      from information_schema.columns
      |      where table_schema = 'public'
      |        and table_name = 'problems'
      |        and column_name = 'owner_username'
      |    ) then
      |      alter table problems rename column owner_username to author_username;
      |    else
      |      alter table problems add column author_username varchar(120);
      |    end if;
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'creator_username'
      |  ) then
      |    update problems
      |    set author_username = coalesce(author_username, creator_username);
      |
      |    alter table problems drop column creator_username;
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'owner_username'
      |  ) then
      |    update problems
      |    set author_username = coalesce(author_username, owner_username);
      |
      |    alter table problems drop column owner_username;
      |  end if;
      |
      |  update problems
      |  set author_username = null
      |  where author_username is not null
      |    and not exists (
      |      select 1
      |      from auth_accounts aa
      |      where aa.username = problems.author_username
      |    );
      |
      |  alter table problems alter column author_username drop not null;
      |
      |  for constraint_record in
      |    select con.conname
      |    from pg_constraint con
      |    join pg_attribute attr
      |      on attr.attrelid = con.conrelid
      |     and attr.attnum = any(con.conkey)
      |    where con.conrelid = 'problems'::regclass
      |      and con.contype = 'f'
      |      and attr.attname = 'author_username'
      |  loop
      |    execute format('alter table problems drop constraint %I', constraint_record.conname);
      |  end loop;
      |
      |  if not exists (
      |    select 1
      |    from pg_constraint
      |    where conname = 'problems_author_username_fkey'
      |      and conrelid = 'problems'::regclass
      |  ) then
      |    alter table problems
      |      add constraint problems_author_username_fkey
      |      foreign key (author_username) references auth_accounts(username) on delete set null;
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

  val addResultDisplayModeColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problems'
      |      and column_name = 'result_display_mode'
      |  ) then
      |    alter table problems add column result_display_mode varchar(32);
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from pg_constraint
      |    where conname = 'problems_result_display_mode_check'
      |      and conrelid = 'problems'::regclass
      |  ) then
      |    alter table problems drop constraint problems_result_display_mode_check;
      |  end if;
      |
      |  update problems
      |  set result_display_mode = 'score'
      |  where result_display_mode is null
      |    or btrim(result_display_mode) = ''
      |    or result_display_mode not in ('verdict', 'score');
      |
      |  if not exists (
      |    select 1
      |    from pg_constraint
      |    where conname = 'problems_result_display_mode_check'
      |      and conrelid = 'problems'::regclass
      |  ) then
      |    alter table problems add constraint problems_result_display_mode_check check (result_display_mode in ('verdict', 'score'));
      |  end if;
      |end $$;
      |""".stripMargin

  val setResultDisplayModeDefaultSql: String =
    """
      |alter table problems
      |alter column result_display_mode set default 'score'
      |""".stripMargin

  val setResultDisplayModeNotNullSql: String =
    """
      |alter table problems
      |alter column result_display_mode set not null
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
        statement.execute(migrateAuthorUsernameColumnSql)
        statement.execute(addBaseAccessColumnSql)
        statement.execute(dropVisibilityColumnSql)
        statement.execute(addDataColumnsSql)
        statement.execute(dropTimeLimitColumnSql)
        statement.execute(dropSpaceLimitColumnSql)
        statement.execute(setReadyDefaultSql)
        statement.execute(setReadyNotNullSql)
        statement.execute(addResultDisplayModeColumnSql)
        statement.execute(setResultDisplayModeDefaultSql)
        statement.execute(setResultDisplayModeNotNullSql)
        statement.execute(setBaseAccessDefaultSql)
        statement.execute(setBaseAccessNotNullSql)
        statement.execute(migrateOtherUserSubmissionAccessColumnSql)
        statement.execute(addOtherUserSubmissionAccessColumnSql)
        statement.execute(setOtherUserSubmissionAccessDefaultSql)
        statement.execute(setOtherUserSubmissionAccessNotNullSql)
        statement.execute(dropStatusColumnSql)
      finally statement.close()
    }
