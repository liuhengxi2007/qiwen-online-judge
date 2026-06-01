package domains.problem.table.problem

import cats.effect.IO
import domains.problem.table.problem_access_grant.ProblemAccessGrantTable

import java.sql.Connection

object ProblemTable:

  def initialize(connection: Connection): IO[Unit] =
    for
      _ <- ProblemTableSchema.initialize(connection)
      _ <- ProblemAccessGrantTable.initialize(connection)
    yield ()
