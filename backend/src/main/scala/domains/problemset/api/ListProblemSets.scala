package domains.problemset.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser

import domains.problemset.objects.response.ProblemSetSummary
import domains.problemset.table.problem_set.ProblemSetTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiPath, HttpApiError, PathParams}
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection

/** 分页列出当前用户可见题单的认证 API。 */
object ListProblemSets extends AuthenticatedApi[PageRequest, PageResponse[ProblemSetSummary]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problem-sets")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[PageResponse[ProblemSetSummary]] = summon[Encoder[PageResponse[ProblemSetSummary]]]

  /** 从查询参数解析分页信息，路径参数不参与列表入口。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[PageRequest] =
    val _ = pathParams
    HttpApiError.fromEitherBadRequest(PageRequestQuerySupport.parsePageRequest(request.uri.query.params))

  /** 按题单基础可见性和授权表读取分页摘要，返回值不包含题目详情。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    pageRequest: PageRequest
  ): IO[PageResponse[ProblemSetSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    ProblemSetTable.listVisibleTo(connection, actor, normalizedPageRequest.page, normalizedPageRequest.pageSize)
