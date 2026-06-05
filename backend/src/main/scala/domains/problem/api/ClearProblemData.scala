package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.utils.ProblemDataStorage
import domains.problem.utils.ProblemDataApiHelpers

import domains.problem.objects.ProblemSlug
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.ProblemDataStateTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, PathParams}

import java.sql.Connection
import java.time.Instant

final case class ClearProblemData(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedApi[ProblemManagementContext, ProblemDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/files/delete-all")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDetail] = summon[Encoder[ProblemDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemManagementContext] =
    ProblemManagementContext.decode(request, pathParams)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    context: ProblemManagementContext
  ): IO[ProblemDetail] =
    ProblemManagementContext
      .requireManagedProblem(connection, actor, context)
      .flatMap(_ => clearManagedProblemData(connection, context.problemSlug))

  def clearManagedProblemData(connection: Connection, problemSlug: ProblemSlug): IO[ProblemDetail] =
    ProblemDataApiHelpers.withProblemForUpdate(connection, problemSlug) { problem =>
      for
        snapshot <- problemDataStorage.snapshotDirectory(problem.slug)
        clearedProblem <- problemDataStorage
          .deleteAllFiles(problem.slug)
          .flatMap(_ => ProblemDataFileTable.deleteAllForProblem(connection, problem.id))
          .flatMap(_ => ProblemDataStateTable.updateData(connection, problem.id, Instant.now(), None))
          .flatMap(_ => ProblemDataApiHelpers.refreshedManagedProblem(connection, problem, "Problem disappeared after clearing data."))
          .handleErrorWith { error =>
            problemDataStorage.restoreDirectory(problem.slug, snapshot) *> IO.raiseError(error)
          }
      yield clearedProblem
    }
