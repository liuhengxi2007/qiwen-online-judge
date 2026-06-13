package domains.judger.table.judger



import cats.effect.IO

import java.sql.Connection

/** judgers 表结构；保存 worker 租约、主机信息和支持语言集合。 */
object JudgerTableSchema:

  val initTableSql: String =
    """
      |create table if not exists judgers (
      |  judger_id varchar(120) primary key,
      |  requested_prefix varchar(120) not null,
      |  host varchar(255) not null,
      |  process_id varchar(120),
      |  supported_languages text not null,
      |  registered_at timestamp not null,
      |  last_heartbeat_at timestamp not null
      |);
      |""".stripMargin

  /** 创建 judger 注册表。 */
  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try statement.execute(initTableSql)
      finally statement.close()
    }
