package domains.auth.table.auth_account



import cats.effect.IO

import java.sql.Connection

/** auth_accounts 表结构和兼容旧字段的迁移脚本集合。 */
object AuthAccountTableSchema:

  val migrateAuthUsersTableSql: String =
    """
      |do $$
      |begin
      |  if exists (
      |    select 1
      |    from information_schema.tables
      |    where table_schema = 'public'
      |      and table_name = 'auth_users'
      |  ) and not exists (
      |    select 1
      |    from information_schema.tables
      |    where table_schema = 'public'
      |      and table_name = 'auth_accounts'
      |  ) then
      |    alter table auth_users rename to auth_accounts;
      |  end if;
      |end $$;
      |""".stripMargin

  val initTableSql: String =
    """
      |create table if not exists auth_accounts (
      |  username varchar(120) primary key,
      |  email varchar(255) not null,
      |  password_hash varchar(255) not null,
      |  site_manager boolean not null default false,
      |  problem_manager boolean not null default false,
      |  contest_manager boolean not null default false
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
      |      and table_name = 'auth_accounts'
      |      and column_name = 'email'
      |  ) and not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_accounts'
      |      and column_name = 'username'
      |  ) then
      |    alter table auth_accounts rename column email to username;
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
      |      and table_name = 'auth_accounts'
      |      and column_name = 'password'
      |  ) then
      |    alter table auth_accounts rename column password to password_hash;
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
      |      and table_name = 'auth_accounts'
      |      and column_name = 'email'
      |  ) then
      |    alter table auth_accounts add column email varchar(255);
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
      |      and table_name = 'auth_accounts'
      |      and column_name = 'display_name'
      |  ) then
      |    alter table auth_accounts alter column display_name set default '';
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_accounts'
      |      and column_name = 'display_mode'
      |  ) then
      |    alter table auth_accounts alter column display_mode set default 'display_name';
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_accounts'
      |      and column_name = 'locale'
      |  ) then
      |    alter table auth_accounts alter column locale set default 'en';
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_accounts'
      |      and column_name = 'problem_title_display_mode'
      |  ) then
      |    alter table auth_accounts alter column problem_title_display_mode set default 'title';
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_accounts'
      |      and column_name = 'auto_mark_message_read'
      |  ) then
      |    alter table auth_accounts alter column auto_mark_message_read set default false;
      |  end if;
      |end $$;
      |""".stripMargin

  val backfillEmailSql: String =
    """
      |update auth_accounts
      |set email = username || '@example.com'
      |where email is null or btrim(email) = ''
      |""".stripMargin

  val setEmailNotNullSql: String =
    """
      |alter table auth_accounts
      |alter column email set not null
      |""".stripMargin

  val createCaseInsensitiveUsernameIndexSql: String =
    """
      |create index if not exists auth_accounts_username_idx
      |on auth_accounts (username)
      |""".stripMargin

  val addSiteManagerColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_accounts'
      |      and column_name = 'site_manager'
      |  ) then
      |    alter table auth_accounts add column site_manager boolean not null default false;
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
      |      and table_name = 'auth_accounts'
      |      and column_name = 'problem_manager'
      |  ) then
      |    alter table auth_accounts add column problem_manager boolean not null default false;
      |  end if;
      |end $$;
      |""".stripMargin

  val addContestManagerColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'auth_accounts'
      |      and column_name = 'contest_manager'
      |  ) then
      |    alter table auth_accounts add column contest_manager boolean not null default false;
      |  end if;
      |end $$;
      |""".stripMargin

  val backfillSiteManagerPermissionFlagsSql: String =
    """
      |update auth_accounts
      |set problem_manager = true,
      |    contest_manager = true
      |where site_manager = true
      |  and (problem_manager = false or contest_manager = false)
      |""".stripMargin

  val addSiteManagerPermissionFlagsConstraintSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from pg_constraint
      |    where conname = 'auth_accounts_site_manager_permission_flags'
      |      and conrelid = 'auth_accounts'::regclass
      |  ) then
      |    alter table auth_accounts
      |    add constraint auth_accounts_site_manager_permission_flags
      |    check (not site_manager or (problem_manager and contest_manager));
      |  end if;
      |end $$;
      |""".stripMargin

  /** 在当前事务连接上执行账号表建表、旧表/旧列迁移、权限约束和邮箱回填。 */
  def initializeSchema(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(migrateAuthUsersTableSql)
        statement.execute(migrateEmailColumnSql)
        statement.execute(migratePasswordColumnSql)
        statement.execute(initTableSql)
        statement.execute(addEmailColumnSql)
        statement.execute(relaxLegacyProfileColumnsSql)
        statement.execute(addSiteManagerColumnSql)
        statement.execute(addProblemManagerColumnSql)
        statement.execute(addContestManagerColumnSql)
        statement.executeUpdate(backfillSiteManagerPermissionFlagsSql)
        statement.execute(addSiteManagerPermissionFlagsConstraintSql)
        statement.executeUpdate(backfillEmailSql)
        statement.execute(setEmailNotNullSql)
        statement.execute(createCaseInsensitiveUsernameIndexSql)
      finally statement.close()
    }
