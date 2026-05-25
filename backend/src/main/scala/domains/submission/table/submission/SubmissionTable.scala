package domains.submission.table.submission

import cats.effect.IO

import java.sql.Connection

object SubmissionTable:

  def initialize(connection: Connection): IO[Unit] =
    SubmissionTableSchema.initialize(connection)
