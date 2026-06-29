package domains.usergroup.table.user_group



import cats.effect.IO

import java.sql.Connection

/** 用户组和成员关系表结构迁移脚本集合。 */
object UserGroupTableSchema:

  val initTableSql: String =
    """
      |create table if not exists user_groups (
      |  id uuid primary key,
      |  slug varchar(64) not null unique,
      |  name varchar(120) not null,
      |  description text not null,
      |  visibility varchar(32) not null default 'private' check (visibility in ('private', 'group', 'public')),
      |  owner_username varchar(120) not null references auth_accounts(username),
      |  created_at timestamp not null,
      |  updated_at timestamp not null
      |);
      |""".stripMargin

  val addVisibilityColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'user_groups'
      |      and column_name = 'visibility'
      |  ) then
      |    alter table user_groups add column visibility varchar(32);
      |  end if;
      |end $$;
      |""".stripMargin

  val backfillVisibilitySql: String =
    """
      |update user_groups
      |set visibility = 'private'
      |where visibility is null or btrim(visibility) = ''
      |""".stripMargin

  val setVisibilityNotNullSql: String =
    """
      |alter table user_groups
      |alter column visibility set not null
      |""".stripMargin

  val setVisibilityDefaultSql: String =
    """
      |alter table user_groups
      |alter column visibility set default 'private'
      |""".stripMargin

  val initMembershipTableSql: String =
    """
      |create table if not exists user_group_memberships (
      |  user_group_id uuid not null references user_groups(id) on delete cascade,
      |  username varchar(120) not null references auth_accounts(username) on delete cascade,
      |  role varchar(32) not null check (role in ('owner', 'manager', 'member')),
      |  joined_at timestamp not null,
      |  primary key (user_group_id, username)
      |);
      |create index if not exists idx_user_group_memberships_username on user_group_memberships(username);
      |""".stripMargin

  /** 建立用户组表、visibility 字段和成员关系表及索引。 */
  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(initTableSql)
        statement.execute(addVisibilityColumnSql)
        statement.executeUpdate(backfillVisibilitySql)
        statement.execute(setVisibilityDefaultSql)
        statement.execute(setVisibilityNotNullSql)
        statement.execute(initMembershipTableSql)
      finally statement.close()
    }
