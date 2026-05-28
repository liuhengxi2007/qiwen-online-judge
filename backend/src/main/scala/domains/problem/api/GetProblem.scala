package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.AuthUser

import domains.problem.objects.ProblemSlug
import domains.problem.objects.response.ProblemDetail
import domains.problem.rules.ProblemAccessRules
import domains.problem.table.problem.ProblemQueryTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object GetProblem extends AuthenticatedApi[ProblemSlug, ProblemDetail]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug")
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
    ProblemQueryTable.findBySlug(connection, problemSlug).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      case Some(problem) =>
        ProblemAccessRules.enrichProblemPermissions(connection, actor, problem).flatMap {
          case Some(enrichedProblem) => IO.pure(enrichedProblem)
          case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
        }
    }
