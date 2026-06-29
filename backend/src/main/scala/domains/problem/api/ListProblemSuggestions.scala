package domains.problem.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser

import domains.problem.objects.request.ProblemSearchQuery
import domains.problem.objects.response.ProblemSuggestion
import domains.problem.table.problem.ProblemQueryTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 列出当前用户可见题目的搜索建议；用于前端输入联想，不返回完整题目内容。 */
object ListProblemSuggestions extends AuthenticatedApi[ProblemSearchQuery, List[ProblemSuggestion]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problem-suggestions")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[List[ProblemSuggestion]] = summon[Encoder[List[ProblemSuggestion]]]

  /** 从 query 参数 q 解析搜索词；空词按 bad request 处理。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemSearchQuery] =
    val _ = pathParams
    HttpApiError.fromEitherBadRequest(ProblemSearchQuery.parse(request.uri.query.params.getOrElse("q", "")))

  /** 基于访问策略返回可见题目建议，输出按匹配度和 slug 排序。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    query: ProblemSearchQuery
  ): IO[List[ProblemSuggestion]] =
    ProblemQueryTable.listSuggestions(connection, actor, query)
