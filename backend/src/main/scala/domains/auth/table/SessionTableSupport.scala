package domains.auth.table

import cats.effect.IO

import java.sql.Timestamp
import java.time.{Duration, Instant}

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
                (
                  resultSet.getString("token"),
                  Option(resultSet.getTimestamp("created_at")).map(_.toInstant).getOrElse(Instant.now()),
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
            val expiresAt = maybeExpiresAt.getOrElse(createdAt.plus(sessionTtl))
            updateStatement.setTimestamp(1, Timestamp.from(lastActiveAt))
            updateStatement.setTimestamp(2, Timestamp.from(expiresAt))
            updateStatement.setString(3, token)
            updateStatement.addBatch()
          }
          if rows.nonEmpty then
            updateStatement.executeBatch()
          ()
        finally updateStatement.close()
      finally selectStatement.close()
    }

  def maxInstant(left: Instant, right: Instant): Instant =
    if left.isAfter(right) then left else right
