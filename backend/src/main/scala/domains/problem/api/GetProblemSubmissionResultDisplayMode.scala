package domains.problem.api

import cats.effect.IO
import domains.problem.objects.ProblemId
import domains.problem.table.problem.ProblemQueryTable
import domains.submission.objects.SubmissionResultDisplayMode

import java.sql.Connection

object GetProblemSubmissionResultDisplayMode:

  def plan(connection: Connection, problemId: ProblemId): IO[Option[SubmissionResultDisplayMode]] =
    ProblemQueryTable.findResultDisplayModeById(connection, problemId)
