package domains.contest.api

import cats.effect.IO
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.problem.objects.ProblemSlug
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.ProblemQueryTable
import shared.api.{ApiMessages, HttpApiError}

import java.sql.Connection

object ContestProblemApiSupport:

  def requireViewLinkedProblem(
    connection: Connection,
    actor: AuthenticatedUser,
    contestSlug: ContestSlug,
    problemSlug: ProblemSlug
  ): IO[ProblemDetail] =
    for
      problem <- loadProblem(connection, problemSlug)
      contestAccess <- loadContestAccess(connection, actor, contestSlug, problem)
      _ <- HttpApiError.ensure(contestAccess.canViewContestDetail, HttpApiError.notFound(ApiMessages.contestNotFound))
      _ <- HttpApiError.ensure(contestAccess.canViewLinkedContestProblem, HttpApiError.notFound(ApiMessages.problemNotFound))
    yield problem.copy(canManage = contestAccess.canManageLinkedContestProblem)

  def requireManageLinkedProblem(
    connection: Connection,
    actor: AuthenticatedUser,
    contestSlug: ContestSlug,
    problemSlug: ProblemSlug
  ): IO[ProblemDetail] =
    for
      problem <- loadProblem(connection, problemSlug)
      contestAccess <- loadContestAccess(connection, actor, contestSlug, problem)
      _ <- HttpApiError.ensure(contestAccess.canManageContest, HttpApiError.forbidden(ApiMessages.contestManagerRequired))
      _ <- HttpApiError.ensure(contestAccess.canManageLinkedContestProblem, HttpApiError.notFound(ApiMessages.contestProblemNotFound))
    yield problem.copy(canManage = true)

  private def loadProblem(connection: Connection, problemSlug: ProblemSlug): IO[ProblemDetail] =
    ProblemQueryTable.findBySlug(connection, problemSlug).flatMap {
      case Some(problem) => IO.pure(problem)
      case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
    }

  private def loadContestAccess(
    connection: Connection,
    actor: AuthenticatedUser,
    contestSlug: ContestSlug,
    problem: ProblemDetail
  ): IO[EvaluateContestAccess.Result] =
    EvaluateContestAccess.plan(connection, actor, EvaluateContestAccess.Input(contestSlug, Some(problem.id))).flatMap {
      case Some(contestAccess) => IO.pure(contestAccess)
      case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
    }
