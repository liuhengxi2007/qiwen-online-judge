package domains.blog.table

import cats.effect.IO

import java.sql.Connection

object BlogTableSchema:

  val initTableSql: String =
    """
      |create table if not exists blogs (
      |  id uuid primary key,
      |  public_id bigint unique not null,
      |  author_username varchar(120) not null references auth_users(username),
      |  title varchar(160) not null,
      |  content text not null,
      |  visibility varchar(16) not null default 'public',
      |  blog_type varchar(16) not null default 'general',
      |  problem_id uuid references problems(id) on delete set null,
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
      |      and table_name = 'blogs'
      |      and column_name = 'visibility'
      |  ) then
      |    alter table blogs add column visibility varchar(16) not null default 'public';
      |  end if;
      |end $$;
      |""".stripMargin

  val addVisibilityCheckSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from pg_constraint
      |    where conname = 'blogs_visibility_check'
      |  ) then
      |    alter table blogs add constraint blogs_visibility_check check (visibility in ('public', 'private'));
      |  end if;
      |end $$;
      |""".stripMargin

  val addBlogTypeColumnsSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'blogs'
      |      and column_name = 'blog_type'
      |  ) then
      |    alter table blogs add column blog_type varchar(16) not null default 'general';
      |  end if;
      |
      |  if not exists (
      |    select 1 from information_schema.columns
      |    where table_schema = 'public'
      |      and table_name = 'blogs'
      |      and column_name = 'problem_id'
      |  ) then
      |    alter table blogs add column problem_id uuid references problems(id) on delete set null;
      |  end if;
      |end $$;
      |""".stripMargin

  val addBlogTypeCheckSql: String =
    """
      |do $$
      |begin
      |  if not exists (
      |    select 1
      |    from pg_constraint
      |    where conname = 'blogs_blog_type_check'
      |  ) then
      |    alter table blogs add constraint blogs_blog_type_check check (blog_type in ('general', 'problem'));
      |  end if;
      |end $$;
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
      |  username varchar(120) not null references auth_users(username) on delete cascade,
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
      |  author_username varchar(120) not null references auth_users(username) on delete cascade,
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
      |  username varchar(120) not null references auth_users(username) on delete cascade,
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

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(createPublicIdSequenceSql)
        statement.execute(initTableSql)
        statement.execute(addVisibilityColumnSql)
        statement.execute(addVisibilityCheckSql)
        statement.execute(addBlogTypeColumnsSql)
        statement.execute(addBlogTypeCheckSql)
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
