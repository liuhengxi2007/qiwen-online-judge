package domains.judger.table



import cats.effect.IO
import domains.judger.http.response.RegisteredJudgerListItem
import judgeprotocol.model.{JudgerId, RegisterJudgerRequest, RegisterJudgerResponse, SubmissionLanguage}
import domains.judger.table.JudgerTableSchema.*
import domains.judger.table.JudgerTableSql.*
import domains.judger.table.utils.JudgerTableSupport.*

import java.sql.{Connection, Timestamp}
import java.time.Instant

object JudgerTable:

  def initialize(connection: Connection): IO[Unit] =
    JudgerTableSchema.initialize(connection)

  def register(
    connection: Connection,
    request: RegisterJudgerRequest,
    heartbeatIntervalMs: Long,
    heartbeatTimeoutMs: Long
  ): IO[RegisterJudgerResponse] =
    for
      now <- IO.realTimeInstant
      _ <- deleteExpired(connection, now.minusMillis(heartbeatTimeoutMs))
      allocatedId <- allocateJudgerId(connection, request.preferredPrefix)
      _ <- IO.blocking {
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
          ()
        finally statement.close()
      }
    yield RegisterJudgerResponse(
      judgerId = allocatedId,
      heartbeatIntervalMs = heartbeatIntervalMs,
      heartbeatTimeoutMs = heartbeatTimeoutMs
    )

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

  def findActiveSupportedLanguages(
    connection: Connection,
    judgerId: JudgerId,
    heartbeatTimeoutMs: Long
  ): IO[Option[List[SubmissionLanguage]]] =
    IO.blocking {
      val now = Instant.now()
      val statement = connection.prepareStatement(findActiveSupportedLanguagesSql)
      try
        statement.setString(1, judgerId.value)
        statement.setTimestamp(2, Timestamp.from(now.minusMillis(heartbeatTimeoutMs)))
        val resultSet = statement.executeQuery()
        try
          if resultSet.next() then Some(parseSupportedLanguages(resultSet.getString("supported_languages")))
          else None
        finally resultSet.close()
      finally statement.close()
    }

  def listJudgers(connection: Connection, heartbeatTimeoutMs: Long): IO[List[RegisteredJudgerListItem]] =
    for
      now <- IO.realTimeInstant
      _ <- deleteExpired(connection, now.minusMillis(heartbeatTimeoutMs))
      judgers <- IO.blocking {
        val statement = connection.prepareStatement(listJudgersSql)
        try
          val resultSet = statement.executeQuery()
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ => readRegisteredJudgerListItem(resultSet))
              .toList
          finally resultSet.close()
        finally statement.close()
      }
    yield judgers
