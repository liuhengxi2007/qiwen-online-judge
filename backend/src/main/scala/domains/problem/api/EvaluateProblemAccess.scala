package domains.problem.api

import cats.effect.IO
import domains.auth.api.InternalOnlyAuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.api.EvaluateContestProblemVisibility
import domains.problem.objects.ProblemSlug
import domains.problem.objects.response.ProblemAccessEvaluationResponse
import domains.problem.utils.ProblemAccessRules
import domains.problem.table.problem.ProblemQueryTable
import domains.usergroup.api.ListUserGroupSlugsForMember
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

object EvaluateProblemAccess extends InternalOnlyAuthenticatedApi[ProblemSlug, ProblemAccessEvaluationResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/problems/evaluate-access")

  override def plan(connection: Connection, actor: AuthenticatedUser, slug: ProblemSlug): IO[ProblemAccessEvaluationResponse] =
    ProblemQueryTable.findBySlug(connection, slug).flatMap {
      case None =>
        IO.pure(ProblemAccessEvaluationResponse(problem = None, canView = false, canManage = false))
      case Some(problem) =>
        for
          actorGroupSlugs <- ListUserGroupSlugsForMember.plan(connection, actor.username)
          hasVisibleContainingProblemSet <- ProblemQueryTable.hasVisibleContainingProblemSet(connection, actor, problem.id)
          contestVisibility <- EvaluateContestProblemVisibility.plan(
            connection,
            actor,
            EvaluateContestProblemVisibility.Input(problem.id, submittedAt = None)
          )
          access = ProblemAccessRules.evaluateProblemPermissions(
            actor,
            problem,
            actorGroupSlugs.slugs.toSet,
            hasVisibleContainingProblemSet,
            contestVisibility.hasVisibleUnfinishedContestContainingProblem,
            contestVisibility.hasVisibleEndedContestContainingProblem
          )
        yield ProblemAccessEvaluationResponse(problem = Some(problem), canView = access.canView, canManage = access.canManage)
    }
