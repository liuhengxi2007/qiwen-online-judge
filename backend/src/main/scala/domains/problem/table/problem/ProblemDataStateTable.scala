package domains.problem.table.problem

import cats.effect.IO
import domains.problem.model.{ProblemDataFilename, ProblemId}

import java.sql.{Connection, Timestamp}
import java.time.Instant

object ProblemDataStateTable:

  def updateData(connection: Connection, problemId: ProblemId, updatedAt: Instant, filename: ProblemDataFilename): IO[Unit] =
    updateData(connection, problemId, updatedAt, Some(filename))

  private val updateDataSQL: String =
    """
      |update problems
      |set data_name = ?, ready = false, updated_at = ?
      |where id = ?
      |""".stripMargin

  def updateData(
    connection: Connection,
    problemId: ProblemId,
    updatedAt: Instant,
    filename: Option[ProblemDataFilename]
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(updateDataSQL)
      try
        filename match
          case Some(value) => statement.setString(1, value.value)
          case None => statement.setNull(1, java.sql.Types.VARCHAR)
        statement.setTimestamp(2, Timestamp.from(updatedAt))
        statement.setObject(3, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }

  private val updateDataReadySQL: String =
    """
      |update problems
      |set data_name = ?, ready = ?, updated_at = ?
      |where id = ?
      |""".stripMargin

  def updateDataReady(
    connection: Connection,
    problemId: ProblemId,
    updatedAt: Instant,
    filename: Option[ProblemDataFilename],
    ready: Boolean
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(updateDataReadySQL)
      try
        filename match
          case Some(value) => statement.setString(1, value.value)
          case None => statement.setNull(1, java.sql.Types.VARCHAR)
        statement.setBoolean(2, ready)
        statement.setTimestamp(3, Timestamp.from(updatedAt))
        statement.setObject(4, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }
