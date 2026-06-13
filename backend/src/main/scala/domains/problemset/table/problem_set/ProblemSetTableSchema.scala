package domains.problemset.table.problem_set



import cats.effect.IO

import java.sql.Connection

/** 题单相关表结构初始化对象，包含历史列迁移和题单题目关系表。 */
object ProblemSetTableSchema:

  val initTableSql: String =
    """
      |create table if not exists problem_sets (
      |  id uuid primary key,
      |  slug varchar(64) not null unique,
      |  title varchar(120) not null,
      |  description text not null,
      |  base_access varchar(32) not null default 'restricted' check (base_access in ('restricted', 'public')),
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
      |      and table_name = 'problem_sets'
      |      and column_name = 'author_username'
      |  ) then
      |    if exists (
      |      select 1
      |      from information_schema.columns
      |      where table_schema = 'public'
      |        and table_name = 'problem_sets'
      |        and column_name = 'creator_username'
      |    ) then
      |      alter table problem_sets rename column creator_username to author_username;
      |    elsif exists (
      |      select 1
      |      from information_schema.columns
      |      where table_schema = 'public'
      |        and table_name = 'problem_sets'
      |        and column_name = 'owner_username'
      |    ) then
      |      alter table problem_sets rename column owner_username to author_username;
      |    else
      |      alter table problem_sets add column author_username varchar(120);
      |    end if;
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problem_sets'
      |      and column_name = 'creator_username'
      |  ) then
      |    update problem_sets
      |    set author_username = coalesce(author_username, creator_username);
      |
      |    alter table problem_sets drop column creator_username;
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'problem_sets'
      |      and column_name = 'owner_username'
      |  ) then
      |    update problem_sets
      |    set author_username = coalesce(author_username, owner_username);
      |
      |    alter table problem_sets drop column owner_username;
      |  end if;
      |
      |  update problem_sets
      |  set author_username = null
      |  where author_username is not null
      |    and not exists (
      |      select 1
      |      from auth_accounts aa
      |      where aa.username = problem_sets.author_username
      |    );
      |
      |  alter table problem_sets alter column author_username drop not null;
      |
      |  for constraint_record in
      |    select con.conname
      |    from pg_constraint con
      |    join pg_attribute attr
      |      on attr.attrelid = con.conrelid
      |     and attr.attnum = any(con.conkey)
      |    where con.conrelid = 'problem_sets'::regclass
      |      and con.contype = 'f'
      |      and attr.attname = 'author_username'
      |  loop
      |    execute format('alter table problem_sets drop constraint %I', constraint_record.conname);
      |  end loop;
      |
      |  if not exists (
      |    select 1
      |    from pg_constraint
      |    where conname = 'problem_sets_author_username_fkey'
      |      and conrelid = 'problem_sets'::regclass
      |  ) then
      |    alter table problem_sets
      |      add constraint problem_sets_author_username_fkey
      |      foreign key (author_username) references auth_accounts(username) on delete set null;
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

  /** 执行题单表、历史作者/可见性迁移和题单题目关系表的幂等初始化 SQL。 */
  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(initTableSql)
        statement.execute(migrateAuthorUsernameColumnSql)
        statement.execute(addBaseAccessColumnSql)
        statement.execute(dropVisibilityColumnSql)
        statement.execute(setBaseAccessDefaultSql)
        statement.execute(setBaseAccessNotNullSql)
        statement.execute(dropStatusColumnSql)
        statement.execute(initProblemRelationTableSql)
      finally statement.close()
    }
