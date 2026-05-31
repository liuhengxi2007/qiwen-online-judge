package domains.submission.table.submission

import cats.effect.IO
import domains.submission.utils.SubmissionProgramStorage

import java.sql.Connection

object SubmissionTable:

  def initialize(connection: Connection, submissionProgramStorage: SubmissionProgramStorage): IO[Unit] =
    SubmissionTableSchema.initialize(connection, submissionProgramStorage)
