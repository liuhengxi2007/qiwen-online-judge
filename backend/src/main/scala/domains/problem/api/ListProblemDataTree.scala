package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.utils.ProblemDataApiHelpers

import domains.problem.objects.response.ProblemDataTreeResponse
import domains.problem.table.problem_data_file.ProblemDataFileTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, PathParams}

import java.sql.Connection

object ListProblemDataTree extends AuthenticatedApi[ProblemManagementContext, ProblemDataTreeResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/files/tree")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDataTreeResponse] = summon[Encoder[ProblemDataTreeResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemManagementContext] =
    ProblemManagementContext.decode(request, pathParams)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    context: ProblemManagementContext
  ): IO[ProblemDataTreeResponse] =
    ProblemManagementContext
      .requireManagedProblem(connection, actor, context)
      .flatMap(problem => listManagedProblemDataTree(connection, problem))

  def listManagedProblemDataTree(connection: Connection, problem: domains.problem.objects.response.ProblemDetail): IO[ProblemDataTreeResponse] =
    ProblemDataFileTable.manifestForProblem(connection, problem.id, problem.slug).map(manifest => ProblemDataApiHelpers.buildTreeResponse(manifest.entries))
