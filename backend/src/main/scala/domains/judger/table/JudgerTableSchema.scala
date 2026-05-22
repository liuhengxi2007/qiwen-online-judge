package domains.judger.table



import cats.effect.IO

import java.sql.Connection

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

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try statement.execute(initTableSql)
      finally statement.close()
    }
