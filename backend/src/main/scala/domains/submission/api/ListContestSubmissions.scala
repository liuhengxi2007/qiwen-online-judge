package domains.submission.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.api.EvaluateContestAccess
import domains.contest.objects.ContestSlug
import domains.submission.objects.request.SubmissionListRequest
import domains.submission.objects.response.SubmissionListResponse
import domains.submission.table.submission.SubmissionQueryTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 列出竞赛提交的 API；竞赛管理员可看全部，普通用户只看自己在该竞赛中的提交。 */
object ListContestSubmissions extends AuthenticatedApi[(ContestSlug, SubmissionListRequest), SubmissionListResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/contests/:contestSlug/submissions")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SubmissionListResponse] = summon[Encoder[SubmissionListResponse]]

  /** 解析竞赛 slug 和提交列表 query 参数。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(ContestSlug, SubmissionListRequest)] =
    for
      contestSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("contestSlug").flatMap(ContestSlug.parse))
      listRequest <- HttpApiError.fromEitherBadRequest(SubmissionListRequest.fromQueryParams(request.uri.query.params))
    yield (contestSlug, listRequest)

  /** 校验竞赛详情可见性后分页返回竞赛提交列表；不可见竞赛统一返回 contest not found。 */
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
      _ <- HttpApiError.ensure(contestAccess.canViewContestDetail, HttpApiError.notFound(ApiMessages.contestNotFound))
      submissions <- SubmissionQueryTable.listVisibleForContest(
        connection,
        actor,
        contestAccess.contestId,
        request,
        canViewAllContestSubmissions = contestAccess.canManageContest
      )
    yield submissions
