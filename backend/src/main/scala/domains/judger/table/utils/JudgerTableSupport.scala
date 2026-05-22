package domains.judger.table.utils



import domains.judger.table.JudgerTableSql
import cats.effect.IO
import domains.judger.http.response.RegisteredJudgerListItem
import judgeprotocol.model.{JudgerId, SubmissionLanguage}

import java.sql.Timestamp
import java.time.Instant

object JudgerTableSupport:

  def deleteExpired(connection: java.sql.Connection, cutoff: Instant): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(JudgerTableSql.deleteExpiredSql)
      try
        statement.setTimestamp(1, Timestamp.from(cutoff))
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  def allocateJudgerId(connection: java.sql.Connection, preferredPrefix: JudgerId): IO[JudgerId] =
    IO.blocking {
      val statement = connection.prepareStatement(JudgerTableSql.listAllocatedIdsSql)
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
    }

  def readRegisteredJudgerListItem(resultSet: java.sql.ResultSet): RegisteredJudgerListItem =
    RegisteredJudgerListItem(
      judgerId = resultSet.getString("judger_id"),
      requestedPrefix = resultSet.getString("requested_prefix"),
      host = resultSet.getString("host"),
      processId = Option(resultSet.getString("process_id")),
      supportedLanguages =
        Option(resultSet.getString("supported_languages"))
          .toList
          .flatMap(_.split(",").toList)
          .map(_.trim)
          .filter(_.nonEmpty),
      registeredAt = resultSet.getTimestamp("registered_at").toInstant,
      lastHeartbeatAt = resultSet.getTimestamp("last_heartbeat_at").toInstant
    )

  def parseSupportedLanguages(raw: String): List[SubmissionLanguage] =
    raw
      .split(",")
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .flatMap {
        case "cpp17" => Some(SubmissionLanguage.Cpp17)
        case "python3" => Some(SubmissionLanguage.Python3)
        case _ => None
      }
