package domains.problem.table



import cats.effect.IO

import java.sql.Connection

object ProblemDataFileTableSchema:

  private val initTableSql =
    """
      |create table if not exists problem_data_files (
      |  problem_id uuid not null references problems(id) on delete cascade,
      |  relative_path varchar(1024) not null,
      |  size_bytes bigint not null,
      |  sha256 varchar(64) not null,
      |  created_at timestamp not null,
      |  primary key (problem_id, relative_path)
      |);
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try statement.execute(initTableSql)
      finally statement.close()
    }
