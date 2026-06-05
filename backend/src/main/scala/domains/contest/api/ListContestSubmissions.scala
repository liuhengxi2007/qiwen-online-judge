package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.ContestSlug
import domains.contest.table.contest.ContestTable
import domains.contest.utils.ContestAccessRules
import domains.submission.objects.request.SubmissionListRequest
import domains.submission.objects.response.SubmissionListResponse
import domains.submission.table.submission.SubmissionQueryTable
import domains.submission.utils.{SubmissionAccessRules, SubmissionListRequestQuery}
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection
import java.time.Instant

object ListContestSubmissions extends AuthenticatedApi[(ContestSlug, SubmissionListRequest), SubmissionListResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/submissions")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SubmissionListResponse] = summon[Encoder[SubmissionListResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, SubmissionListRequest)] =
    for
      contestSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))
      listRequest = SubmissionListRequestQuery.parse(request.uri.query.params)
    yield (contestSlug, listRequest)

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (ContestSlug, SubmissionListRequest)
  ): IO[SubmissionListResponse] =
    val (contestSlug, request) = input
    for
      maybeContest <- ContestTable.findBySlug(connection, contestSlug)
      contest <- maybeContest match
        case Some(contest) => IO.pure(contest)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      actorGroupSlugs <- ListUserGroupSlugsForMember.plan(connection, actor.username)
      canViewContest = ContestAccessRules.canViewContest(actor, contest, actorGroupSlugs.slugs.toSet)
      canManageContest = ContestAccessRules.canManageContest(actor, contest, actorGroupSlugs.slugs.toSet)
      _ <- HttpApiError.ensure(canViewContest, HttpApiError.notFound(ApiMessages.contestNotFound))
      registration <- ContestTable.findRegistration(connection, contest.id, actor.username)
      now = Instant.now()
      registeredBeforeStart = registration.exists(registeredAt => !registeredAt.isAfter(contest.startAt))
      canViewContestSubmissions = canManageContest || now.isAfter(contest.endAt) || registeredBeforeStart
      _ <- HttpApiError.ensure(canViewContestSubmissions, HttpApiError.forbidden(ApiMessages.contestNotRegistered))
      submissions <- SubmissionQueryTable.listVisibleForContest(
        connection,
        actor,
        contest.id,
        request,
        SubmissionAccessRules.hasGlobalViewOverride(actor),
        canViewAllContestSubmissions = canManageContest
      )
    yield submissions
