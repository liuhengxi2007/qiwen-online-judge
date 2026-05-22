package domains.auth.table.utils



import domains.auth.table.SessionTableSql
import cats.effect.IO
import domains.auth.model.SessionToken

import java.sql.Timestamp
import java.time.Duration

object SessionTableSupport:

  def backfillSessionTimestamps(connection: java.sql.Connection, sessionTtl: Duration): IO[Unit] =
    IO.blocking {
      val selectStatement = connection.prepareStatement(SessionTableSql.selectMissingTimestampsSql)
      try
        val resultSet = selectStatement.executeQuery()
        val rows =
          try
            Iterator
              .continually(resultSet.next())
              .takeWhile(identity)
              .map(_ =>
                val token = SessionToken
                  .parse(resultSet.getString("token"))
                  .fold(message => throw new IllegalStateException(message), identity)
                (
                  token,
                  Option(resultSet.getTimestamp("created_at"))
                    .map(_.toInstant)
                    .getOrElse(throw new IllegalStateException(s"auth_sessions.created_at is missing for token ${token.value}")),
                  Option(resultSet.getTimestamp("last_active_at")).map(_.toInstant),
                  Option(resultSet.getTimestamp("expires_at")).map(_.toInstant),
                )
              )
              .toList
          finally resultSet.close()

        val updateStatement = connection.prepareStatement(SessionTableSql.backfillTimestampsSql)
        try
          rows.foreach { (token, createdAt, maybeLastActiveAt, maybeExpiresAt) =>
            val lastActiveAt = maybeLastActiveAt.getOrElse(createdAt)
            val expiresAt = maybeExpiresAt.getOrElse(lastActiveAt.plus(sessionTtl))
            updateStatement.setTimestamp(1, Timestamp.from(lastActiveAt))
            updateStatement.setTimestamp(2, Timestamp.from(expiresAt))
            updateStatement.setString(3, token.value)
            updateStatement.addBatch()
          }
          if rows.nonEmpty then
            updateStatement.executeBatch()
          ()
        finally updateStatement.close()
      finally selectStatement.close()
    }
