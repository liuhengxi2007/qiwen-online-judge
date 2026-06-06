package domains.problem.table.problem

import cats.effect.IO
import domains.problem.objects.{ProblemDataFilename, ProblemId}
import domains.submission.objects.SubmissionResultDisplayMode

import java.sql.{Connection, Timestamp}
import java.time.Instant

object ProblemDataStateTable:

  def updateData(connection: Connection, problemId: ProblemId, updatedAt: Instant, filename: ProblemDataFilename): IO[Unit] =
    updateData(connection, problemId, updatedAt, Some(filename))

  private val updateDataSQL: String =
    """
      |update problems
      |set data_name = ?, ready = false, result_display_mode = 'score', updated_at = ?
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
      |set data_name = ?, ready = ?, result_display_mode = ?, updated_at = ?
      |where id = ?
      |""".stripMargin

  def updateDataReady(
    connection: Connection,
    problemId: ProblemId,
    updatedAt: Instant,
    filename: Option[ProblemDataFilename],
    ready: Boolean,
    resultDisplayMode: SubmissionResultDisplayMode = SubmissionResultDisplayMode.Score
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(updateDataReadySQL)
      try
        filename match
          case Some(value) => statement.setString(1, value.value)
          case None => statement.setNull(1, java.sql.Types.VARCHAR)
        statement.setBoolean(2, ready)
        statement.setString(3, SubmissionResultDisplayMode.encode(resultDisplayMode))
        statement.setTimestamp(4, Timestamp.from(updatedAt))
        statement.setObject(5, problemId.value)
        statement.executeUpdate()
        ()
      finally statement.close()
    }
