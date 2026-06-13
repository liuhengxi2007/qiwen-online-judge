package domains.submission.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.submission.utils.SubmissionAccessRules
import domains.submission.utils.SubmissionListRequestQuery

import domains.submission.objects.request.SubmissionListRequest
import domains.submission.objects.response.SubmissionListResponse
import domains.submission.table.submission.SubmissionQueryTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 列出全站可见提交的 API；可见性受题目提交公开策略、本人身份和题目管理员权限约束。 */
object ListSubmissions extends AuthenticatedApi[SubmissionListRequest, SubmissionListResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/submissions")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SubmissionListResponse] = summon[Encoder[SubmissionListResponse]]

  /** 从 query 参数解析筛选、排序和分页；非法筛选值返回 400。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[SubmissionListRequest] =
    val _ = pathParams
    HttpApiError.fromEitherBadRequest(SubmissionListRequestQuery.parse(request.uri.query.params))

  /** 返回调用者可见的提交摘要页，详情可见性单独标在每条记录上。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, request: SubmissionListRequest): IO[SubmissionListResponse] =
    SubmissionQueryTable.listVisibleTo(connection, actor, request, SubmissionAccessRules.hasGlobalViewOverride(actor))
