package domains.problemset.table.problem_set



import cats.effect.IO

import java.sql.Connection

object ProblemSetTableSchema:

  val initTableSql: String =
    """
      |create table if not exists problem_sets (
      |  id uuid primary key,
      |  slug varchar(64) not null unique,
      |  title varchar(120) not null,
      |  description text not null,
      |  base_access varchar(32) not null default 'restricted' check (base_access in ('restricted', 'public')),
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
      |      and table_name = 'problem_sets'
      |      and column_name = 'owner_username'
      |  ) and not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problem_sets'
      |      and column_name = 'creator_username'
      |  ) then
      |    alter table problem_sets rename column owner_username to creator_username;
      |  end if;
      |end $$;
      |""".stripMargin

  val initProblemRelationTableSql: String =
    """
      |create table if not exists problem_set_problems (
      |  problem_set_id uuid not null references problem_sets(id) on delete cascade,
      |  problem_id uuid not null references problems(id) on delete cascade,
      |  position integer not null,
      |  primary key (problem_set_id, problem_id),
      |  unique (problem_set_id, position)
      |);
      |""".stripMargin

  val addBaseAccessColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problem_sets'
      |      and column_name = 'base_access'
      |  ) then
      |    alter table problem_sets add column base_access varchar(32);
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from pg_constraint
      |    where conname = 'problem_sets_base_access_check'
      |      and conrelid = 'problem_sets'::regclass
      |  ) then
      |    alter table problem_sets drop constraint problem_sets_base_access_check;
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problem_sets'
      |      and column_name = 'visibility'
      |  ) then
      |    update problem_sets
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
      |    update problem_sets
      |    set base_access = 'restricted'
      |    where base_access is null
      |      or btrim(base_access) = ''
      |      or base_access not in ('restricted', 'public');
      |  end if;
      |
      |  if not exists (
      |    select 1
      |    from pg_constraint
      |    where conname = 'problem_sets_base_access_check'
      |      and conrelid = 'problem_sets'::regclass
      |  ) then
      |    alter table problem_sets add constraint problem_sets_base_access_check check (base_access in ('restricted', 'public'));
      |  end if;
      |end $$;
      |""".stripMargin

  val dropVisibilityColumnSql: String =
    """
      |alter table problem_sets
      |drop column if exists visibility
      |""".stripMargin

  val setBaseAccessNotNullSql: String =
    """
      |alter table problem_sets
      |alter column base_access set not null
      |""".stripMargin

  val setBaseAccessDefaultSql: String =
    """
      |alter table problem_sets
      |alter column base_access set default 'restricted'
      |""".stripMargin

  val dropStatusColumnSql: String =
    """
      |alter table problem_sets
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
        statement.execute(setBaseAccessDefaultSql)
        statement.execute(setBaseAccessNotNullSql)
        statement.execute(dropStatusColumnSql)
        statement.execute(initProblemRelationTableSql)
      finally statement.close()
    }
