package domains.problem.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.problem.objects.{ProblemId, ProblemSlug}
import domains.problem.objects.internal.ProblemDataManifest
import domains.problem.table.problem.ProblemQueryTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

final case class JudgeProblemDataManifestInput(
  problemId: ProblemId,
  problemSlug: ProblemSlug
)

object GetJudgeProblemDataManifest extends InternalOnlyApi[JudgeProblemDataManifestInput, Option[ProblemDataManifest]]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/problems/judge-data-manifest")

  def input(problemId: ProblemId, problemSlug: ProblemSlug): JudgeProblemDataManifestInput =
    JudgeProblemDataManifestInput(problemId = problemId, problemSlug = problemSlug)

  override def plan(connection: Connection, input: JudgeProblemDataManifestInput): IO[Option[ProblemDataManifest]] =
    ProblemQueryTable.findBySlug(connection, input.problemSlug).flatMap {
      case Some(problem) if problem.id == input.problemId =>
        ProblemDataFileTable
          .manifestForProblem(connection, input.problemId, input.problemSlug)
          .map(Some(_))
      case _ =>
        IO.pure(None)
    }
