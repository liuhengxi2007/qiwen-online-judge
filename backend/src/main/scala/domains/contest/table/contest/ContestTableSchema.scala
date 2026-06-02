package domains.contest.table.contest

import cats.effect.IO

import java.sql.Connection

object ContestTableSchema:

  val initTableSql: String =
    """
      |create table if not exists contests (
      |  id uuid primary key,
      |  slug varchar(64) not null unique,
      |  title varchar(120) not null,
      |  description text not null default '',
      |  start_at timestamp not null,
      |  end_at timestamp not null,
      |  base_access varchar(32) not null default 'restricted',
      |  author_username varchar(120) references auth_accounts(username) on delete set null,
      |  created_at timestamp not null,
      |  updated_at timestamp not null,
      |  constraint contests_base_access_check check (base_access in ('restricted', 'public')),
      |  constraint contests_time_range_check check (end_at > start_at)
      |);
      |""".stripMargin

  val initProblemRelationTableSql: String =
    """
      |create table if not exists contest_problems (
      |  contest_id uuid not null references contests(id) on delete cascade,
      |  problem_id uuid not null references problems(id) on delete restrict,
      |  position integer not null,
      |  alias varchar(8) not null,
      |  primary key (contest_id, problem_id),
      |  unique (contest_id, position),
      |  unique (contest_id, alias),
      |  constraint contest_problems_position_check check (position >= 1)
      |);
      |""".stripMargin

  val initRegistrationTableSql: String =
    """
      |create table if not exists contest_registrations (
      |  contest_id uuid not null references contests(id) on delete cascade,
      |  username varchar(120) not null references auth_accounts(username) on delete cascade,
      |  registered_at timestamp not null,
      |  primary key (contest_id, username)
      |);
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try
        statement.execute(initTableSql)
        statement.execute(initProblemRelationTableSql)
        statement.execute(initRegistrationTableSql)
      finally statement.close()
    }
