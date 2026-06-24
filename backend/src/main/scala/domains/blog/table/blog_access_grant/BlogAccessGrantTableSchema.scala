package domains.blog.table.blog_access_grant

import cats.effect.IO

import java.sql.Connection

/** 博客查看授权表结构初始化对象，包含历史通用 viewer 授权回填。 */
object BlogAccessGrantTableSchema:

  val initTableSql: String =
    """
      |create table if not exists blog_access_grants (
      |  blog_id uuid not null references blogs(id) on delete cascade,
      |  subject_kind varchar(32) not null check (subject_kind in ('user', 'user_group')),
      |  subject_key varchar(64) not null,
      |  created_at timestamp not null,
      |  primary key (blog_id, subject_kind, subject_key)
      |);
      |create index if not exists idx_blog_access_grants_subject
      |  on blog_access_grants(subject_kind, subject_key);
      |""".stripMargin

  val backfillResourceAccessViewerGrantsSql: String =
    """
      |insert into blog_access_grants (blog_id, subject_kind, subject_key, created_at)
      |select rag.resource_id, rag.subject_kind, rag.subject_key, rag.created_at
      |from resource_access_grants rag
      |join blogs b on b.id = rag.resource_id
      |where rag.resource_kind = 'blog'
      |  and rag.grant_role = 'viewer'
      |on conflict (blog_id, subject_kind, subject_key) do nothing
      |""".stripMargin

  val backfillResourceViewerGrantsSql: String =
    """
      |insert into blog_access_grants (blog_id, subject_kind, subject_key, created_at)
      |select rvg.resource_id, rvg.subject_kind, rvg.subject_key, rvg.created_at
      |from resource_viewer_grants rvg
      |join blogs b on b.id = rvg.resource_id
      |where rvg.resource_kind = 'blog'
      |on conflict (blog_id, subject_kind, subject_key) do nothing
      |""".stripMargin

  val deleteBackfilledResourceAccessGrantsSql: String =
    """
      |delete from resource_access_grants
      |where resource_kind = 'blog'
      |""".stripMargin

  val deleteBackfilledResourceViewerGrantsSql: String =
    """
      |delete from resource_viewer_grants
      |where resource_kind = 'blog'
      |""".stripMargin

  /** 创建博客授权表，并从旧 resource_* 授权表迁移 blog viewer 记录后删除旧记录。 */
  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(initTableSql)
        if tableExists(connection, "resource_access_grants") then
          statement.executeUpdate(backfillResourceAccessViewerGrantsSql)
          statement.executeUpdate(deleteBackfilledResourceAccessGrantsSql)
        if tableExists(connection, "resource_viewer_grants") then
          statement.executeUpdate(backfillResourceViewerGrantsSql)
          statement.executeUpdate(deleteBackfilledResourceViewerGrantsSql)
      finally statement.close()
    }

  private def tableExists(connection: Connection, tableName: String): Boolean =
    Option(connection.getMetaData.getTables(null, null, tableName, null)).exists { resultSet =>
      try resultSet.next()
      finally resultSet.close()
    }
