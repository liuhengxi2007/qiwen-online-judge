package domains.problem.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.problem.utils.ProblemDataStorage
import domains.problem.http.ProblemDataApiSupport
import domains.problem.http.codec.ProblemHttpCodecs.given
import domains.problem.objects.ProblemSlug
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.ProblemDataStateTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

final case class ClearProblemData(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedApi[ProblemSlug, ProblemDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/clear")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDetail] = summon[Encoder[ProblemDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemSlug] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))

  override def plan(
    connection: Connection,
    actor: AuthUser,
    problemSlug: ProblemSlug
  ): IO[ProblemDetail] =
    ProblemDataApiSupport.withManageableProblemForUpdate(connection, actor, problemSlug) { problem =>
      for
        snapshot <- problemDataStorage.snapshotDirectory(problem.slug)
        clearedProblem <- problemDataStorage
          .deleteAllFiles(problem.slug)
          .flatMap(_ => ProblemDataFileTable.deleteAllForProblem(connection, problem.id))
          .flatMap(_ => ProblemDataStateTable.updateData(connection, problem.id, Instant.now(), None))
          .flatMap(_ => ProblemDataApiSupport.refreshedManagedProblem(connection, problem, "Problem disappeared after clearing data."))
          .handleErrorWith { error =>
            problemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
          }
      yield clearedProblem
    }
