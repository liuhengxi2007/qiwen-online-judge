package domains.problem.api

import cats.effect.IO
import database.table.resource_access_grant.ResourceAccessGrantTable
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.problem.objects.ProblemSlug
import domains.problem.rules.ProblemAccessRules
import domains.problem.table.problem.{ProblemMutationTable, ProblemQueryTable}
import io.circe.Encoder
import org.http4s.{Method, Request, Status}

import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.access.{ResourceId, ResourceKind}
import shared.objects.response.SuccessResponse

import java.sql.Connection

object DeleteProblem extends AuthenticatedApi[ProblemSlug, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/delete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemSlug] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))

  override def plan(
    connection: Connection,
    actor: AuthUser,
    problemSlug: ProblemSlug
  ): IO[SuccessResponse] =
    ProblemQueryTable.findBySlug(connection, problemSlug).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      case Some(problem) =>
        for
          canManage <- ProblemAccessRules.canManageProblem(connection, actor, problem)
          _ <- HttpApiError.ensure(canManage, HttpApiError.notFound(ApiMessages.problemNotFound))
          _ <- ResourceAccessGrantTable.deleteAllForResource(connection, ResourceKind.Problem, ResourceId(problem.id.value))
          _ <- ProblemMutationTable.delete(connection, problem.id)
        yield SuccessResponse(code = Some(ApiMessages.problemDeleted.code), message = None, params = ApiMessages.problemDeleted.params)
    }
