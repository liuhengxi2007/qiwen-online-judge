package domains.user.table.user

import cats.effect.IO

import java.sql.Connection

object UserTableSchema:

  val initTableSql: String =
    """
      |create table if not exists user_profiles (
      |  username varchar(120) primary key references auth_users(username) on delete cascade,
      |  display_name varchar(120) not null,
      |  display_mode varchar(64) not null default 'display_name',
      |  locale varchar(32) not null default 'en',
      |  problem_title_display_mode varchar(64) not null default 'title',
      |  auto_mark_message_read boolean not null default false
      |);
      |""".stripMargin

  val backfillFromAuthAccountTableSql: String =
    """
      |do $$
      |declare
      |  display_name_expression text;
      |  display_mode_expression text;
      |  locale_expression text;
      |  problem_title_display_mode_expression text;
      |  auto_mark_message_read_expression text;
      |begin
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'display_name'
      |  ) then
      |    display_name_expression := 'coalesce(nullif(btrim(display_name), ''''), case when lower(username) = ''admin'' then ''Admin User'' else username end)';
      |  else
      |    display_name_expression := 'case when lower(username) = ''admin'' then ''Admin User'' else username end';
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'display_mode'
      |  ) then
      |    display_mode_expression := 'coalesce(nullif(display_mode, ''''), ''display_name'')';
      |  else
      |    display_mode_expression := '''display_name''';
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'locale'
      |  ) then
      |    locale_expression := 'coalesce(nullif(locale, ''''), ''en'')';
      |  else
      |    locale_expression := '''en''';
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'problem_title_display_mode'
      |  ) then
      |    problem_title_display_mode_expression := 'coalesce(nullif(problem_title_display_mode, ''''), ''title'')';
      |  else
      |    problem_title_display_mode_expression := '''title''';
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'auto_mark_message_read'
      |  ) then
      |    auto_mark_message_read_expression := 'coalesce(auto_mark_message_read, false)';
      |  else
      |    auto_mark_message_read_expression := 'false';
      |  end if;
      |
      |  execute format(
      |    'insert into user_profiles (username, display_name, display_mode, locale, problem_title_display_mode, auto_mark_message_read)
      |     select username, %s, %s, %s, %s, %s
      |     from auth_users
      |     on conflict (username) do nothing',
      |    display_name_expression,
      |    display_mode_expression,
      |    locale_expression,
      |    problem_title_display_mode_expression,
      |    auto_mark_message_read_expression
      |  );
      |end $$;
      |""".stripMargin

  val dropLegacyAuthAccountProfileColumnsSql: String =
    """
      |alter table auth_users
      |  drop column if exists display_name,
      |  drop column if exists display_mode,
      |  drop column if exists locale,
      |  drop column if exists problem_title_display_mode,
      |  drop column if exists auto_mark_message_read
      |""".stripMargin

  def initializeSchema(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(initTableSql)
        statement.execute(backfillFromAuthAccountTableSql)
        statement.execute(dropLegacyAuthAccountProfileColumnsSql)
      finally statement.close()
    }
