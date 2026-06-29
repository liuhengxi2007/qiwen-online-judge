package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser

import domains.problem.objects.request.{ProblemListRequest, ProblemSearchQuery}
import domains.problem.objects.response.ProblemSummary
import domains.problem.table.problem.ProblemQueryTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiPath, HttpApiError, PathParams}
import shared.objects.PageResponse

import java.sql.Connection

/** 分页列出当前用户可见题目的 API；支持可选搜索词和分页参数。 */
object ListProblems extends AuthenticatedApi[ProblemListRequest, PageResponse[ProblemSummary]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[PageResponse[ProblemSummary]] = summon[Encoder[PageResponse[ProblemSummary]]]

  /** 从 query 参数解析搜索和分页；非法搜索词返回 400。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemListRequest] =
    val _ = pathParams
    val queryParams = request.uri.query.params
    HttpApiError.fromEitherBadRequest {
      for
        query <- queryParams.get("q").map(rawQuery => ProblemSearchQuery.parse(rawQuery).map(Some(_))).getOrElse(Right(None))
        pageRequest <- PageRequestQuerySupport.parsePageRequest(queryParams)
      yield ProblemListRequest(query = query, pageRequest = pageRequest)
    }

  /** 归一化分页后查询可见题目列表，输出包含总数和当前页。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    request: ProblemListRequest
  ): IO[PageResponse[ProblemSummary]] =
    val normalizedRequest = request.copy(pageRequest = request.pageRequest.normalized)
    ProblemQueryTable.listVisibleTo(connection, actor, normalizedRequest)
