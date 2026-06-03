package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.contest.table.contest.ContestTable
import domains.contest.utils.ContestAccessRules
import domains.problem.objects.ProblemSlug
import domains.problem.objects.response.ProblemDetail
import domains.problem.table.problem.ProblemQueryTable
import domains.problem.utils.ProblemAccessRules
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

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
    val now = Instant.now()
    for
      maybeContest <- ContestTable.findBySlug(connection, contestSlug)
      contest <- maybeContest match
        case Some(contest) => IO.pure(contest)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      actorGroupSlugs <- ListUserGroupSlugsForMember.plan(connection, actor.username)
      _ <- HttpApiError.ensure(
        ContestAccessRules.canViewContest(actor, contest, actorGroupSlugs.slugs.toSet),
        HttpApiError.notFound(ApiMessages.contestNotFound)
      )
      canManageContest = ContestAccessRules.canManageContest(actor, contest, actorGroupSlugs.slugs.toSet)
      registration <- ContestTable.findRegistration(connection, contest.id, actor.username)
      registeredBeforeStart = registration.exists(registeredAt => !registeredAt.isAfter(contest.startAt))
      canOpenContestProblem = canManageContest || now.isAfter(contest.endAt) || (!now.isBefore(contest.startAt) && registeredBeforeStart)
      _ <- HttpApiError.ensure(canOpenContestProblem, HttpApiError.forbidden(ApiMessages.contestNotRegistered))
      problem <- ProblemQueryTable.findBySlug(connection, problemSlug).flatMap {
        case Some(problem) => IO.pure(problem)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      }
      _ <- HttpApiError.ensure(
        contest.problems.exists(contestProblem => contestProblem.id.value == problem.id.value),
        HttpApiError.notFound(ApiMessages.problemNotFound)
      )
      canManageProblem = ProblemAccessRules.canManageProblem(actor, problem, actorGroupSlugs.slugs.toSet)
    yield problem.copy(canManage = canManageProblem)
