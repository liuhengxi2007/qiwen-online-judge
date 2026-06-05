package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.utils.ProblemDataStorage

import domains.problem.objects.response.ProblemDataFileListResponse
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, PathParams}

import java.sql.Connection

final case class ListProblemDataFiles(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedApi[ProblemManagementContext, ProblemDataFileListResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/files")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDataFileListResponse] = summon[Encoder[ProblemDataFileListResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemManagementContext] =
    ProblemManagementContext.decode(request, pathParams)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    context: ProblemManagementContext
  ): IO[ProblemDataFileListResponse] =
    ProblemManagementContext
      .requireManagedProblem(connection, actor, context)
      .flatMap(problem => problemDataStorage.listFiles(problem.slug).map(ProblemDataFileListResponse(_)))
