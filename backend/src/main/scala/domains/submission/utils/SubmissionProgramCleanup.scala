package domains.submission.utils

import cats.effect.IO
import cats.syntax.all.*
import domains.problem.objects.ProblemId
import domains.submission.table.submission.SubmissionQueryTable

import java.sql.Connection

object SubmissionProgramCleanup:

  def prepareDeleteForProblem(
    connection: Connection,
    problemId: ProblemId,
    submissionProgramStorage: SubmissionProgramStorage
  ): IO[IO[Unit]] =
    SubmissionQueryTable
      .listProgramManifestsForProblem(connection, problemId)
      .map(_.traverse_(manifest => submissionProgramStorage.deleteManifest(manifest).handleError(_ => ())))
