package domains.problemset.application



import cats.effect.IO
import domains.problemset.model.ProblemSetSlug
import domains.problemset.table.problem_set.ProblemSetTable

import java.sql.Connection

object ProblemSetCommands:
  export ProblemSetCommandResults.*
  export ProblemSetQueryCommands.*
  export ProblemSetMutationCommands.*
  export ProblemSetRelationCommands.*

  def problemSetSlugConflictsWith(connection: Connection, rawValue: String): IO[Boolean] =
    ProblemSetSlug.parse(rawValue) match
      case Left(_) => IO.pure(false)
      case Right(slug) => ProblemSetTable.findBySlug(connection, slug).map(_.nonEmpty)
