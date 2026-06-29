package domains.contest.table.contest_access_grant

import cats.effect.IO

import java.sql.Connection

/** 比赛访问授权表结构初始化对象。 */
object ContestAccessGrantTableSchema:

  val initTableSql: String =
    """
      |create table if not exists contest_access_grants (
      |  contest_id uuid not null references contests(id) on delete cascade,
      |  grant_role varchar(16) not null,
      |  subject_kind varchar(32) not null,
      |  subject_key varchar(255) not null,
      |  created_at timestamp not null,
      |  primary key (contest_id, grant_role, subject_kind, subject_key),
      |  constraint contest_access_grants_role_check check (grant_role in ('viewer', 'manager')),
      |  constraint contest_access_grants_subject_check check (subject_kind in ('user', 'user_group'))
      |);
      |""".stripMargin

  val createSubjectIndexSql: String =
    """
      |create index if not exists contest_access_grants_subject_idx
      |on contest_access_grants (subject_kind, subject_key, grant_role)
      |""".stripMargin

  /** 执行比赛授权表和主体索引的幂等初始化 SQL。 */
  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(initTableSql)
        statement.execute(createSubjectIndexSql)
      finally statement.close()
    }
