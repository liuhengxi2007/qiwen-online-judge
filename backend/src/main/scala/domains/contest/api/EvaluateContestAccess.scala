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
    registeredBeforeStart: Boolean,
    containsProblem: Boolean,
    canViewContest: Boolean,
    canManageContest: Boolean
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
          registration <- ContestTable.findRegistration(connection, contest.id, actor.username)
          now = Instant.now()
          actorGroupSlugSet = actorGroupSlugs.slugs.toSet
          containsProblem = input.problemId.forall(problemId => contest.problems.exists(_.id.value == problemId.value))
        yield Some(
          Result(
            contestId = contest.id,
            contestSlug = contest.slug,
            contestTitle = contest.title,
            contestStarted = !now.isBefore(contest.startAt),
            contestEnded = now.isAfter(contest.endAt),
            registeredBeforeStart = registration.exists(registeredAt => !registeredAt.isAfter(contest.startAt)),
            containsProblem = containsProblem,
            canViewContest = ContestAccessRules.canViewContest(actor, contest, actorGroupSlugSet),
            canManageContest = ContestAccessRules.canManageContest(actor, contest, actorGroupSlugSet)
          )
        )
    }
