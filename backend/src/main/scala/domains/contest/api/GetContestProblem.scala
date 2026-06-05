package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.problem.api.EvaluateProblemAccess
import domains.problem.objects.ProblemSlug
import domains.problem.objects.response.ProblemDetail
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection
object GetContestProblem extends AuthenticatedApi[(ContestSlug, ProblemSlug), ProblemDetail]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/problems/:problemSlug")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ProblemDetail] = summon[Encoder[ProblemDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, ProblemSlug)] =
    val _ = request
    for
      contestSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))
      problemSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))
    yield (contestSlug, problemSlug)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, ProblemSlug)
  ): IO[ProblemDetail] =
    val (contestSlug, problemSlug) = input
    for
      problemAccess <- EvaluateProblemAccess.plan(connection, actor, problemSlug)
      problem <- problemAccess.problem match
        case Some(problem) => IO.pure(problem)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      maybeContestAccess <- EvaluateContestAccess.plan(connection, actor, EvaluateContestAccess.Input(contestSlug, Some(problem.id)))
      contestAccess <- maybeContestAccess match
        case Some(contestAccess) => IO.pure(contestAccess)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      _ <- HttpApiError.ensure(contestAccess.canViewContest, HttpApiError.notFound(ApiMessages.contestNotFound))
      canOpenContestProblem =
        contestAccess.canManageContest ||
          contestAccess.contestEnded ||
          (contestAccess.contestStarted && contestAccess.registeredBeforeStart)
      _ <- HttpApiError.ensure(canOpenContestProblem, HttpApiError.forbidden(ApiMessages.contestNotRegistered))
      _ <- HttpApiError.ensure(
        contestAccess.containsProblem,
        HttpApiError.notFound(ApiMessages.problemNotFound)
      )
    yield problem.copy(canManage = problemAccess.canManage)
