package domains.problem.table.problem

import cats.effect.IO

import java.sql.Connection

object ProblemTable:

  def initialize(connection: Connection): IO[Unit] =
    ProblemTableSchema.initialize(connection)
