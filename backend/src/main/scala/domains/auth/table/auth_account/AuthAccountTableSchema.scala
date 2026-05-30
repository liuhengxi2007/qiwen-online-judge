package domains.auth.table.auth_account



import cats.effect.IO

import java.sql.Connection

object AuthAccountTableSchema:

  val initTableSql: String =
    """
      |create table if not exists auth_users (
      |  username varchar(120) primary key,
      |  email varchar(255) not null,
      |  password_hash varchar(255) not null,
      |  site_manager boolean not null default false,
      |  problem_manager boolean not null default false
      |);
      |""".stripMargin

  val migrateEmailColumnSql: String =
    """
      |do $$
      |begin
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'email'
      |  ) and not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'username'
      |  ) then
      |    alter table auth_users rename column email to username;
      |  end if;
      |end $$;
      |""".stripMargin

  val migratePasswordColumnSql: String =
    """
      |do $$
      |begin
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'password'
      |  ) then
      |    alter table auth_users rename column password to password_hash;
      |  end if;
      |end $$;
      |""".stripMargin

  val addEmailColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'email'
      |  ) then
      |    alter table auth_users add column email varchar(255);
      |  end if;
      |end $$;
      |""".stripMargin

  val relaxLegacyProfileColumnsSql: String =
    """
      |do $$
      |begin
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'display_name'
      |  ) then
      |    alter table auth_users alter column display_name set default '';
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'display_mode'
      |  ) then
      |    alter table auth_users alter column display_mode set default 'display_name';
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'locale'
      |  ) then
      |    alter table auth_users alter column locale set default 'en';
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'problem_title_display_mode'
      |  ) then
      |    alter table auth_users alter column problem_title_display_mode set default 'title';
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'auto_mark_message_read'
      |  ) then
      |    alter table auth_users alter column auto_mark_message_read set default false;
      |  end if;
      |end $$;
      |""".stripMargin

  val backfillEmailSql: String =
    """
      |update auth_users
      |set email = username || '@example.com'
      |where email is null or btrim(email) = ''
      |""".stripMargin

  val setEmailNotNullSql: String =
    """
      |alter table auth_users
      |alter column email set not null
      |""".stripMargin

  val createCaseInsensitiveUsernameIndexSql: String =
    """
      |create index if not exists auth_users_username_idx
      |on auth_users (username)
      |""".stripMargin

  val addSiteManagerColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'site_manager'
      |  ) then
      |    alter table auth_users add column site_manager boolean not null default false;
      |  end if;
      |end $$;
      |""".stripMargin

  val addProblemManagerColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |      and column_name = 'problem_manager'
      |  ) then
      |    alter table auth_users add column problem_manager boolean not null default false;
      |  end if;
      |end $$;
      |""".stripMargin

  def initializeSchema(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(migrateEmailColumnSql)
        statement.execute(migratePasswordColumnSql)
        statement.execute(initTableSql)
        statement.execute(addEmailColumnSql)
        statement.execute(relaxLegacyProfileColumnsSql)
        statement.execute(addSiteManagerColumnSql)
        statement.execute(addProblemManagerColumnSql)
        statement.executeUpdate(backfillEmailSql)
        statement.execute(setEmailNotNullSql)
        statement.execute(createCaseInsensitiveUsernameIndexSql)
      finally statement.close()
    }
