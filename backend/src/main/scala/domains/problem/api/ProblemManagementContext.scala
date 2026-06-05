package domains.problem.api

import cats.effect.IO
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.api.EvaluateContestAccess
import domains.contest.objects.ContestSlug
import domains.problem.objects.ProblemSlug
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.ProblemQueryTable
import org.http4s.Request
import shared.api.{ApiMessages, HttpApiError, PathParams}

import java.sql.Connection

final case class ProblemManagementContext(
  problemSlug: ProblemSlug,
  contestSlug: Option[ContestSlug]
)

object ProblemManagementContext:

  def decode(request: Request[IO], pathParams: PathParams): IO[ProblemManagementContext] =
    for
      problemSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("problemSlug").flatMap(ProblemSlug.parse))
      contestSlug <- parseOptionalContestSlug(request)
    yield ProblemManagementContext(problemSlug, contestSlug)

  def parseOptionalContestSlug(request: Request[IO]): IO[Option[ContestSlug]] =
    request.uri.query.params.get("contestSlug") match
      case Some(rawContestSlug) =>
        HttpApiError.fromEitherBadRequest(ContestSlug.parse(rawContestSlug).map(Some(_)))
      case None =>
        IO.pure(None)

  def requireManagedProblem(
    connection: Connection,
    actor: AuthenticatedUser,
    context: ProblemManagementContext
  ): IO[ProblemDetail] =
    context.contestSlug match
      case Some(contestSlug) =>
        requireContestManagedProblem(connection, actor, context.problemSlug, contestSlug)
      case None =>
        requireDirectManagedProblem(connection, actor, context.problemSlug)

  def requireContestManagementIfPresent(
    connection: Connection,
    actor: AuthenticatedUser,
    contestSlug: Option[ContestSlug]
  ): IO[Unit] =
    contestSlug match
      case Some(slug) =>
        loadContestAccess(connection, actor, slug, problem = None).flatMap { access =>
          HttpApiError.ensure(access.canManageContest, HttpApiError.forbidden(ApiMessages.contestManagerRequired))
        }
      case None =>
        IO.unit

  private def requireDirectManagedProblem(
    connection: Connection,
    actor: AuthenticatedUser,
    problemSlug: ProblemSlug
  ): IO[ProblemDetail] =
    EvaluateProblemAccess.plan(connection, actor, problemSlug).flatMap { access =>
      access.problem match
        case Some(problem) =>
          HttpApiError.ensure(access.canManage, HttpApiError.notFound(ApiMessages.problemNotFound)).map(_ => problem.copy(canManage = true))
        case None =>
          HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
    }

  private def requireContestManagedProblem(
    connection: Connection,
    actor: AuthenticatedUser,
    problemSlug: ProblemSlug,
    contestSlug: ContestSlug
  ): IO[ProblemDetail] =
    for
      problem <- ProblemQueryTable.findBySlug(connection, problemSlug).flatMap {
        case Some(problem) => IO.pure(problem)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      }
      contestAccess <- loadContestAccess(connection, actor, contestSlug, problem = Some(problem))
      _ <- HttpApiError.ensure(contestAccess.canManageContest, HttpApiError.forbidden(ApiMessages.contestManagerRequired))
      _ <- HttpApiError.ensure(contestAccess.containsProblem, HttpApiError.notFound(ApiMessages.contestProblemNotFound))
    yield problem.copy(canManage = true)

  private def loadContestAccess(
    connection: Connection,
    actor: AuthenticatedUser,
    contestSlug: ContestSlug,
    problem: Option[ProblemDetail]
  ): IO[EvaluateContestAccess.Result] =
    EvaluateContestAccess.plan(connection, actor, EvaluateContestAccess.Input(contestSlug, problem.map(_.id))).flatMap {
      case Some(contestAccess) => IO.pure(contestAccess)
      case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
    }
