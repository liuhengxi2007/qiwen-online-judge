package domains.contest.api

import cats.effect.IO
import domains.auth.api.InternalOnlyAuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.table.contest.ContestProblemVisibilityTable
import domains.problem.objects.ProblemId
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection
import java.time.Instant

object EvaluateContestProblemVisibility
  extends InternalOnlyAuthenticatedApi[EvaluateContestProblemVisibility.Input, EvaluateContestProblemVisibility.Result]:

  final case class Input(
    problemId: ProblemId,
    submittedAt: Option[Instant]
  )

  final case class Result(
    hasVisibleUnfinishedContestContainingProblem: Boolean,
    hasVisibleEndedContestContainingProblem: Boolean,
    hasRegisteredContestContainingSubmission: Boolean
  )

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/contests/evaluate-problem-visibility")

  override def plan(connection: Connection, actor: AuthenticatedUser, input: Input): IO[Result] =
    for
      hasVisibleUnfinishedContestContainingProblem <- ContestProblemVisibilityTable
        .hasVisibleUnfinishedContestContainingProblem(connection, actor, input.problemId)
      hasVisibleEndedContestContainingProblem <- ContestProblemVisibilityTable
        .hasVisibleEndedContestContainingProblem(connection, actor, input.problemId)
      hasRegisteredContestContainingSubmission <- input.submittedAt match
        case Some(submittedAt) =>
          ContestProblemVisibilityTable.hasRegisteredContestContainingSubmission(connection, actor, input.problemId, submittedAt)
        case None =>
          IO.pure(false)
    yield Result(
      hasVisibleUnfinishedContestContainingProblem = hasVisibleUnfinishedContestContainingProblem,
      hasVisibleEndedContestContainingProblem = hasVisibleEndedContestContainingProblem,
      hasRegisteredContestContainingSubmission = hasRegisteredContestContainingSubmission
    )
