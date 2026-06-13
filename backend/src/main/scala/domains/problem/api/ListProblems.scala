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
import shared.api.{ApiPath, PathParams}
import shared.objects.PageResponse

import java.sql.Connection

/** 分页列出当前用户可见题目的 API；支持可选搜索词和分页参数。 */
object ListProblems extends AuthenticatedApi[ProblemListRequest, PageResponse[ProblemSummary]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[PageResponse[ProblemSummary]] = summon[Encoder[PageResponse[ProblemSummary]]]

  /** 从 query 参数解析搜索和分页；非法搜索词会被忽略为空过滤。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemListRequest] =
    val _ = pathParams
    val queryParams = request.uri.query.params
    IO.pure(
      ProblemListRequest(
        // FIXME-CN: 非法 q 会被静默丢弃为无搜索过滤，用户输入错误和“不过滤”无法区分。
        query = queryParams.get("q").flatMap(rawQuery => ProblemSearchQuery.parse(rawQuery).toOption),
        pageRequest = PageRequestQuerySupport.parsePageRequest(queryParams)
      )
    )

  /** 归一化分页后查询可见题目列表，输出包含总数和当前页。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    request: ProblemListRequest
  ): IO[PageResponse[ProblemSummary]] =
    val normalizedRequest = request.copy(pageRequest = request.pageRequest.normalized)
    ProblemQueryTable.listVisibleTo(connection, actor, normalizedRequest)
