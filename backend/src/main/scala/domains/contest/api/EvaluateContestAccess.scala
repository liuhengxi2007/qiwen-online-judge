package domains.contest.api

import cats.effect.IO
import domains.auth.api.InternalOnlyAuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.{ContestId, ContestSlug, ContestTitle}
import domains.contest.table.contest.ContestTable
import domains.contest.utils.ContestAccessRules
import domains.problem.objects.ProblemId
import domains.usergroup.api.ListUserGroupSlugsForMember
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection
import java.time.Instant

object EvaluateContestAccess extends InternalOnlyAuthenticatedApi[EvaluateContestAccess.Input, Option[EvaluateContestAccess.Result]]:

  final case class Input(
    contestSlug: ContestSlug,
    problemId: Option[ProblemId]
  )

  final case class Result(
    contestId: ContestId,
    contestSlug: ContestSlug,
    contestTitle: ContestTitle,
    contestStarted: Boolean,
    contestEnded: Boolean,
    isRegistered: Boolean,
    containsProblem: Boolean,
    canViewContest: Boolean,
    canViewContestDetail: Boolean,
    canManageContest: Boolean,
    canViewLinkedContestProblem: Boolean,
    canManageLinkedContestProblem: Boolean,
    canSubmitContestProblem: Boolean
  )

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/contests/evaluate-access")

  override def plan(connection: Connection, actor: AuthenticatedUser, input: Input): IO[Option[Result]] =
    ContestTable.findBySlug(connection, input.contestSlug).flatMap {
      case None =>
        IO.pure(None)
      case Some(contest) =>
        for
          actorGroupSlugs <- ListUserGroupSlugsForMember.plan(connection, actor.username)
          isRegistered <- ContestTable.isRegistered(connection, contest.id, actor.username)
          now = Instant.now()
          actorGroupSlugSet = actorGroupSlugs.slugs.toSet
          containsProblem = input.problemId.exists(problemId => contest.problems.exists(_.id.value == problemId.value))
          canViewContest = ContestAccessRules.canViewContest(actor, contest, actorGroupSlugSet)
          canViewContestDetail = ContestAccessRules.canViewContestDetail(actor, contest, actorGroupSlugSet, isRegistered, now)
          canManageContest = ContestAccessRules.canManageContest(actor, contest, actorGroupSlugSet)
          canViewLinkedContestProblem =
            ContestAccessRules.canViewLinkedContestProblem(actor, contest, actorGroupSlugSet, isRegistered, containsProblem, now)
          canManageLinkedContestProblem =
            ContestAccessRules.canManageLinkedContestProblem(actor, contest, actorGroupSlugSet, containsProblem)
          canSubmitContestProblem =
            ContestAccessRules.canSubmitContestProblem(contest, isRegistered, containsProblem, now)
        yield Some(
          Result(
            contestId = contest.id,
            contestSlug = contest.slug,
            contestTitle = contest.title,
            contestStarted = !now.isBefore(contest.startAt),
            contestEnded = now.isAfter(contest.endAt),
            isRegistered = isRegistered,
            containsProblem = containsProblem,
            canViewContest = canViewContest,
            canViewContestDetail = canViewContestDetail,
            canManageContest = canManageContest,
            canViewLinkedContestProblem = canViewLinkedContestProblem,
            canManageLinkedContestProblem = canManageLinkedContestProblem,
            canSubmitContestProblem = canSubmitContestProblem
          )
        )
    }
