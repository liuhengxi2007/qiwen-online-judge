package domains.blog.table.blog



import cats.effect.IO

import java.sql.Connection

/** 博客相关表结构初始化对象，包含博客、访问策略、题目关联、投票、评论和历史字段迁移。 */
object BlogTableSchema:

  val initTableSql: String =
    """
      |create table if not exists blogs (
      |  id uuid primary key,
      |  public_id bigint unique not null,
      |  author_username varchar(120) not null references auth_accounts(username),
      |  title varchar(160) not null,
      |  content text not null,
      |  base_access varchar(32) not null default 'public' check (base_access in ('restricted', 'public')),
      |  created_at timestamp not null,
      |  updated_at timestamp not null
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
      |      and table_name = 'blogs'
      |      and column_name = 'base_access'
      |  ) then
      |    alter table blogs add column base_access varchar(32);
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from pg_constraint
      |    where conname = 'blogs_base_access_check'
      |      and conrelid = 'blogs'::regclass
      |  ) then
      |    alter table blogs drop constraint blogs_base_access_check;
      |  end if;
      |
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'blogs'
      |      and column_name = 'visibility'
      |  ) then
      |    update blogs
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
      |    update blogs
      |    set base_access = 'public'
      |    where base_access is null
      |      or btrim(base_access) = ''
      |      or base_access not in ('restricted', 'public');
      |  end if;
      |
      |  alter table blogs alter column base_access set default 'public';
      |  alter table blogs alter column base_access set not null;
      |
      |  if not exists (
      |    select 1
      |    from pg_constraint
      |    where conname = 'blogs_base_access_check'
      |      and conrelid = 'blogs'::regclass
      |  ) then
      |    alter table blogs add constraint blogs_base_access_check check (base_access in ('restricted', 'public'));
      |  end if;
      |end $$;
      |""".stripMargin

  val dropVisibilityColumnSql: String =
    """
      |do $$
      |begin
      |  alter table blogs drop constraint if exists blogs_visibility_check;
      |  alter table blogs drop column if exists visibility;
      |end $$;
      |""".stripMargin

  val initProblemLinkTableSql: String =
    """
      |create table if not exists blog_problem_links (
      |  blog_id uuid not null references blogs(id) on delete cascade,
      |  problem_id uuid not null references problems(id) on delete cascade,
      |  linked_by varchar(120) not null references auth_accounts(username),
      |  linked_at timestamp not null,
      |  status varchar(16) not null default 'accepted',
      |  primary key (blog_id, problem_id)
      |);
      |""".stripMargin

  val addProblemLinkStatusColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'blog_problem_links'
      |      and column_name = 'status'
      |  ) then
      |    alter table blog_problem_links add column status varchar(16) not null default 'accepted';
      |  end if;
      |end $$;
      |""".stripMargin

  val addProblemLinkStatusCheckSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from pg_constraint
      |    where conname = 'blog_problem_links_status_check'
      |  ) then
      |    alter table blog_problem_links add constraint blog_problem_links_status_check check (status in ('pending', 'accepted'));
      |  end if;
      |end $$;
      |""".stripMargin

  val migrateLegacyProblemLinksSql: String =
    """
      |do $$
      |begin
      |  if exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'blogs'
      |      and column_name = 'problem_id'
      |  ) then
      |    insert into blog_problem_links (blog_id, problem_id, linked_by, linked_at, status)
      |    select id, problem_id, author_username, created_at, 'accepted'
      |    from blogs
      |    where problem_id is not null
      |    on conflict (blog_id, problem_id) do nothing;
      |  end if;
      |end $$;
      |""".stripMargin

  val dropLegacyProblemColumnsSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.tables
      |    where table_schema = 'public'
      |      and table_name = 'blogs'
      |  ) then
      |    return;
      |  end if;
      |
      |  alter table blogs drop constraint if exists blogs_blog_type_check;
      |  alter table blogs drop column if exists problem_id;
      |  alter table blogs drop column if exists blog_type;
      |end $$;
      |""".stripMargin

  val createProblemLinkProblemIndexSql: String =
    """
      |create index if not exists blog_problem_links_problem_id_idx on blog_problem_links(problem_id, linked_at desc)
      |""".stripMargin

  val createProblemLinkBlogIndexSql: String =
    """
      |create index if not exists blog_problem_links_blog_id_idx on blog_problem_links(blog_id)
      |""".stripMargin

  val createPublicIdSequenceSql: String =
    """
      |create sequence if not exists blog_public_id_seq start with 1 increment by 1
      |""".stripMargin

  val addPublicIdDefaultSql: String =
    """
      |alter table blogs
      |alter column public_id set default nextval('blog_public_id_seq')
      |""".stripMargin

  val createCreatedAtIndexSql: String =
    """
      |create index if not exists blogs_created_at_idx on blogs(created_at desc, public_id desc)
      |""".stripMargin

  val initVoteTableSql: String =
    """
      |create table if not exists blog_votes (
      |  blog_id uuid not null references blogs(id) on delete cascade,
      |  username varchar(120) not null references auth_accounts(username) on delete cascade,
      |  vote varchar(16) not null,
      |  created_at timestamp not null,
      |  updated_at timestamp not null,
      |  primary key (blog_id, username),
      |  constraint blog_votes_vote_check check (vote in ('up', 'down'))
      |);
      |""".stripMargin

  val createVoteBlogIndexSql: String =
    """
      |create index if not exists blog_votes_blog_id_idx on blog_votes(blog_id)
      |""".stripMargin

  val createCommentPublicIdSequenceSql: String =
    """
      |create sequence if not exists blog_comment_public_id_seq start with 1 increment by 1
      |""".stripMargin

  val initCommentTableSql: String =
    """
      |create table if not exists blog_comments (
      |  id uuid primary key,
      |  public_id bigint unique not null default nextval('blog_comment_public_id_seq'),
      |  blog_id uuid not null references blogs(id) on delete cascade,
      |  parent_comment_id uuid references blog_comments(id) on delete cascade,
      |  author_username varchar(120) not null references auth_accounts(username) on delete cascade,
      |  content text not null,
      |  created_at timestamp not null,
      |  updated_at timestamp not null
      |);
      |""".stripMargin

  val addParentCommentColumnSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'blog_comments'
      |      and column_name = 'parent_comment_id'
      |  ) then
      |    alter table blog_comments add column parent_comment_id uuid references blog_comments(id) on delete cascade;
      |  end if;
      |end $$;
      |""".stripMargin

  val initCommentVoteTableSql: String =
    """
      |create table if not exists blog_comment_votes (
      |  comment_id uuid not null references blog_comments(id) on delete cascade,
      |  username varchar(120) not null references auth_accounts(username) on delete cascade,
      |  vote varchar(16) not null,
      |  created_at timestamp not null,
      |  updated_at timestamp not null,
      |  primary key (comment_id, username),
      |  constraint blog_comment_votes_vote_check check (vote in ('up', 'down'))
      |);
      |""".stripMargin

  val createCommentIndexSql: String =
    """
      |create index if not exists blog_comments_blog_id_idx on blog_comments(blog_id, public_id asc)
      |""".stripMargin

  /** 执行博客 domain 所有表、序列、索引和历史迁移的幂等初始化 SQL。 */
  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(createPublicIdSequenceSql)
        statement.execute(initTableSql)
        statement.execute(addBaseAccessColumnSql)
        statement.execute(dropVisibilityColumnSql)
        statement.execute(initProblemLinkTableSql)
        statement.execute(addProblemLinkStatusColumnSql)
        statement.execute(addProblemLinkStatusCheckSql)
        statement.execute(migrateLegacyProblemLinksSql)
        statement.execute(dropLegacyProblemColumnsSql)
        statement.execute(createProblemLinkProblemIndexSql)
        statement.execute(createProblemLinkBlogIndexSql)
        statement.execute(addPublicIdDefaultSql)
        statement.execute(createCreatedAtIndexSql)
        statement.execute(initVoteTableSql)
        statement.execute(createVoteBlogIndexSql)
        statement.execute(createCommentPublicIdSequenceSql)
        statement.execute(initCommentTableSql)
        statement.execute(addParentCommentColumnSql)
        statement.execute(initCommentVoteTableSql)
        statement.execute(createCommentIndexSql)
      finally statement.close()
    }
