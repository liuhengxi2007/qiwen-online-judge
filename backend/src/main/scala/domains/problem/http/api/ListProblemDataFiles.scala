package domains.problem.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.problem.application.ProblemDataStorage
import domains.problem.http.codec.ProblemHttpCodecs.given
import domains.problem.objects.ProblemSlug
import domains.problem.objects.response.ProblemDataFileListResponse
import domains.problem.rules.ProblemAccessRules
import domains.problem.table.problem.ProblemQueryTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

final case class ListProblemDataFiles(problemDataStorage: ProblemDataStorage)
    extends AuthenticatedApi[ProblemSlug, ProblemDataFileListResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDataFileListResponse] = summon[Encoder[ProblemDataFileListResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemSlug] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))

  override def plan(
    connection: Connection,
    actor: AuthUser,
    problemSlug: ProblemSlug
  ): IO[ProblemDataFileListResponse] =
    ProblemQueryTable.findBySlug(connection, problemSlug).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      case Some(problem) =>
        for
          canManage <- ProblemAccessRules.canManageProblem(connection, actor, problem)
          _ <- HttpApiError.ensure(canManage, HttpApiError.notFound(ApiMessages.problemNotFound))
          files <- problemDataStorage.listFiles(problem.slug)
        yield ProblemDataFileListResponse(files)
    }
