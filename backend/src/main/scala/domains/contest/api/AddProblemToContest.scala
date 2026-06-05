package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.contest.objects.request.AddProblemToContestRequest
import domains.contest.objects.response.{ContestDetail, ContestRegistrationStatus}
import domains.contest.table.contest.ContestTable
import domains.contest.table.contest.ContestTable.AddProblemTableResult
import domains.contest.utils.ContestAccessRules
import domains.problem.api.EvaluateProblemAccess
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object AddProblemToContest extends AuthenticatedApi[(ContestSlug, AddProblemToContestRequest), ContestDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/problems")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[ContestDetail] = summon[Encoder[ContestDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, AddProblemToContestRequest)] =
    for
      contestSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))
      body <- request.as[AddProblemToContestRequest]
    yield (contestSlug, body)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, AddProblemToContestRequest)
  ): IO[ContestDetail] =
    val (contestSlug, request) = input
    for
      maybeContest <- ContestTable.findBySlug(connection, contestSlug)
      contest <- maybeContest match
        case Some(contest) => IO.pure(contest)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      actorGroupSlugs <- ListUserGroupSlugsForMember.plan(connection, actor.username)
      _ <- HttpApiError.ensure(
        ContestAccessRules.canManageContest(actor, contest, actorGroupSlugs.slugs.toSet),
        HttpApiError.forbidden(ApiMessages.contestManagerRequired)
      )
      problemAccess <- EvaluateProblemAccess.plan(connection, actor, request.problemSlug)
      problem <- problemAccess.problem match
        case Some(problem) if problemAccess.canManage => IO.pure(problem)
        case _ => HttpApiError.raise(HttpApiError.notFound(ApiMessages.problemNotFound))
      _ <- ContestTable.addProblem(connection, contest.id, problem.id).flatMap {
        case AddProblemTableResult.Linked => IO.unit
        case AddProblemTableResult.AlreadyLinked =>
          HttpApiError.raise(HttpApiError.conflict(ApiMessages.problemAlreadyLinkedToContest))
      }
      updatedContest <- ContestTable.findBySlug(connection, contest.slug).flatMap {
        case Some(contest) => IO.pure(contest)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      }
      isRegistered <- ContestTable.isRegistered(connection, updatedContest.id, actor.username)
    yield ContestDetail.fromContest(
      updatedContest,
      if isRegistered then ContestRegistrationStatus.registered else ContestRegistrationStatus.notRegistered,
      canManage = true,
      includeProblems = true
    )
