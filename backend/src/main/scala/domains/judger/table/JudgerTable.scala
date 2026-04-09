package domains.judger.table

import cats.effect.IO
import judgeprotocol.model.{JudgerId, RegisterJudgerRequest, RegisterJudgerResponse, SubmissionLanguage}

import java.sql.{Connection, Timestamp}
import java.time.Instant

object JudgerTable:

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

  private val deleteExpiredSql: String =
    """
      |delete from judgers
      |where last_heartbeat_at < ?
      |""".stripMargin

  private val listAllocatedIdsSql: String =
    """
      |select judger_id
      |from judgers
      |where requested_prefix = ?
      |order by judger_id asc
      |""".stripMargin

  private val insertSql: String =
    """
      |insert into judgers (judger_id, requested_prefix, host, process_id, supported_languages, registered_at, last_heartbeat_at)
      |values (?, ?, ?, ?, ?, ?, ?)
      |""".stripMargin

  private val heartbeatSql: String =
    """
      |update judgers
      |set last_heartbeat_at = ?
      |where judger_id = ?
      |  and last_heartbeat_at >= ?
      |""".stripMargin

  def initialize(connection: Connection): IO[Unit] =
    IO.blocking {
      val statement = connection.createStatement()
      try statement.execute(initTableSql)
      finally statement.close()
    }

  def register(
    connection: Connection,
    request: RegisterJudgerRequest,
    heartbeatIntervalMs: Long,
    heartbeatTimeoutMs: Long
  ): IO[RegisterJudgerResponse] =
    IO.blocking {
      val now = Instant.now()
      deleteExpired(connection, now.minusMillis(heartbeatTimeoutMs))
      val allocatedId = allocateJudgerId(connection, request.preferredPrefix)

      val statement = connection.prepareStatement(insertSql)
      try
        statement.setString(1, allocatedId.value)
        statement.setString(2, request.preferredPrefix.value)
        statement.setString(3, request.host)
        request.processId match
          case Some(processId) => statement.setString(4, processId)
          case None => statement.setNull(4, java.sql.Types.VARCHAR)
        statement.setString(5, request.supportedLanguages.map(SubmissionLanguage.render).mkString(","))
        statement.setTimestamp(6, Timestamp.from(now))
        statement.setTimestamp(7, Timestamp.from(now))
        statement.executeUpdate()
      finally statement.close()

      RegisterJudgerResponse(
        judgerId = allocatedId,
        heartbeatIntervalMs = heartbeatIntervalMs,
        heartbeatTimeoutMs = heartbeatTimeoutMs
      )
    }

  def heartbeat(connection: Connection, judgerId: JudgerId, heartbeatTimeoutMs: Long): IO[Boolean] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(heartbeatSql)
      try
        statement.setTimestamp(1, Timestamp.from(now))
        statement.setString(2, judgerId.value)
        statement.setTimestamp(3, Timestamp.from(now.minusMillis(heartbeatTimeoutMs)))
        statement.executeUpdate() > 0
      finally statement.close()
    }

  private def deleteExpired(connection: Connection, cutoff: Instant): Unit =
    val statement = connection.prepareStatement(deleteExpiredSql)
    try
      statement.setTimestamp(1, Timestamp.from(cutoff))
      statement.executeUpdate()
    finally statement.close()

  private def allocateJudgerId(connection: Connection, preferredPrefix: JudgerId): JudgerId =
    val statement = connection.prepareStatement(listAllocatedIdsSql)
    try
      statement.setString(1, preferredPrefix.value)
      val resultSet = statement.executeQuery()
      try
        val allocatedIds =
          Iterator
            .continually(resultSet.next())
            .takeWhile(identity)
            .map(_ => resultSet.getString("judger_id"))
            .toSet

        Iterator
          .from(1)
          .map { index =>
            if index == 1 then preferredPrefix.value
            else s"${preferredPrefix.value}-$index"
          }
          .find(id => !allocatedIds.contains(id))
          .flatMap(JudgerId.parse(_).toOption)
          .getOrElse(throw new IllegalStateException("Failed to allocate judger id."))
      finally resultSet.close()
    finally statement.close()
