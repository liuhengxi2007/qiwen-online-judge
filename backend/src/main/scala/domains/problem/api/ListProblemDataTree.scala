package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.problem.utils.ProblemDataApiHelpers

import domains.problem.objects.ProblemSlug
import domains.problem.objects.response.ProblemDataTreeResponse
import domains.problem.rules.ProblemAccessRules
import domains.problem.table.problem.ProblemQueryTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object ListProblemDataTree extends AuthenticatedApi[ProblemSlug, ProblemDataTreeResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/data/tree")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDataTreeResponse] = summon[Encoder[ProblemDataTreeResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemSlug] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))

  override def plan(
    connection: Connection,
    actor: AuthUser,
    problemSlug: ProblemSlug
  ): IO[ProblemDataTreeResponse] =
    ProblemQueryTable.findBySlug(connection, problemSlug).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      case Some(problem) =>
        for
          canManage <- ProblemAccessRules.canManageProblem(connection, actor, problem)
          _ <- HttpApiError.ensure(canManage, HttpApiError.notFound(ApiMessages.problemNotFound))
          manifest <- ProblemDataFileTable.manifestForProblem(connection, problem.id, problem.slug)
        yield ProblemDataApiHelpers.buildTreeResponse(manifest.entries)
    }
