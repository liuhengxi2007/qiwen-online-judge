package domains.problem.table.problem_access_grant

import cats.effect.IO

import java.sql.Connection

object ProblemAccessGrantTableSchema:

  val initTableSql: String =
    """
      |create table if not exists problem_access_grants (
      |  problem_id uuid not null references problems(id) on delete cascade,
      |  grant_role varchar(32) not null check (grant_role in ('viewer', 'manager')),
      |  subject_kind varchar(32) not null check (subject_kind in ('user', 'user_group')),
      |  subject_key varchar(64) not null,
      |  created_at timestamp not null,
      |  primary key (problem_id, grant_role, subject_kind, subject_key)
      |);
      |create index if not exists idx_problem_access_grants_subject
      |  on problem_access_grants(grant_role, subject_kind, subject_key);
      |""".stripMargin

  val backfillResourceAccessGrantsSql: String =
    """
      |insert into problem_access_grants (problem_id, grant_role, subject_kind, subject_key, created_at)
      |select rag.resource_id, rag.grant_role, rag.subject_kind, rag.subject_key, rag.created_at
      |from resource_access_grants rag
      |join problems p on p.id = rag.resource_id
      |where rag.resource_kind = 'problem'
      |on conflict (problem_id, grant_role, subject_kind, subject_key) do nothing
      |""".stripMargin

  val backfillResourceViewerGrantsSql: String =
    """
      |insert into problem_access_grants (problem_id, grant_role, subject_kind, subject_key, created_at)
      |select rvg.resource_id, 'viewer', rvg.subject_kind, rvg.subject_key, rvg.created_at
      |from resource_viewer_grants rvg
      |join problems p on p.id = rvg.resource_id
      |where rvg.resource_kind = 'problem'
      |on conflict (problem_id, grant_role, subject_kind, subject_key) do nothing
      |""".stripMargin

  val deleteBackfilledResourceAccessGrantsSql: String =
    """
      |delete from resource_access_grants
      |where resource_kind = 'problem'
      |""".stripMargin

  val deleteBackfilledResourceViewerGrantsSql: String =
    """
      |delete from resource_viewer_grants
      |where resource_kind = 'problem'
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(initTableSql)
        if tableExists(connection, "resource_access_grants") then
          statement.executeUpdate(backfillResourceAccessGrantsSql)
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
