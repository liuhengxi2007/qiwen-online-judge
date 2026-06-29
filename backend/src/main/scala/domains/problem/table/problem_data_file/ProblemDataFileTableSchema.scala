package domains.problem.table.problem_data_file



import cats.effect.IO

import java.sql.Connection

/** problem_data_files 表结构；保存题目数据 manifest 的持久化条目。 */
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

  /** 创建题目数据文件清单表。 */
  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try statement.execute(initTableSql)
      finally statement.close()
    }
