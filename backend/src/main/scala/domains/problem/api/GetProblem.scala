package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser

import domains.problem.objects.ProblemSlug
import domains.problem.objects.response.ProblemDetail
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
    actor: AuthenticatedUser,
    problemSlug: ProblemSlug
  ): IO[ProblemDetail] =
    EvaluateProblemAccess.plan(connection, actor, problemSlug).flatMap { access =>
      access.problem match
        case Some(problem) if access.canView =>
          IO.pure(problem.copy(canManage = access.canManage))
        case _ =>
          HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
    }
