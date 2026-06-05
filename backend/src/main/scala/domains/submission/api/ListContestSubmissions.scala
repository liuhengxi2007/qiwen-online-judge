package domains.submission.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.api.EvaluateContestAccess
import domains.contest.objects.ContestSlug
import domains.submission.objects.request.SubmissionListRequest
import domains.submission.objects.response.SubmissionListResponse
import domains.submission.table.submission.SubmissionQueryTable
import domains.submission.utils.{SubmissionAccessRules, SubmissionListRequestQuery}
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

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
      maybeContestAccess <- EvaluateContestAccess.plan(connection, actor, EvaluateContestAccess.Input(contestSlug, problemId = None))
      contestAccess <- maybeContestAccess match
        case Some(contestAccess) => IO.pure(contestAccess)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.contestNotFound))
      _ <- HttpApiError.ensure(contestAccess.canViewContest, HttpApiError.notFound(ApiMessages.contestNotFound))
      canViewContestSubmissions = contestAccess.canManageContest || contestAccess.contestEnded || contestAccess.registeredBeforeStart
      _ <- HttpApiError.ensure(canViewContestSubmissions, HttpApiError.forbidden(ApiMessages.contestNotRegistered))
      submissions <- SubmissionQueryTable.listVisibleForContest(
        connection,
        actor,
        contestAccess.contestId,
        request,
        SubmissionAccessRules.hasGlobalViewOverride(actor),
        canViewAllContestSubmissions = contestAccess.canManageContest || contestAccess.contestEnded
      )
    yield submissions
