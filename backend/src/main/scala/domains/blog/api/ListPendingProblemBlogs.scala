package domains.blog.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser


import domains.blog.objects.response.{BlogListResponse, BlogSummary}
import domains.blog.table.blog.BlogProblemLinkQueryTable
import domains.problem.objects.ProblemSlug
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiPath, HttpApiError, PathParams}
import shared.objects.PageResponse

import java.sql.Connection

/** 分页列出题目待审核博客提交的认证 API，仅题目目录管理员可看到真实列表。 */
object ListPendingProblemBlogs extends AuthenticatedApi[ProblemBlogsInput, BlogListResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/blog-submissions")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[BlogListResponse] = summon[Encoder[BlogListResponse]]

  /** 从路径解析题目 slug，并从查询参数解析分页信息。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemBlogsInput] =
    HttpApiError.fromEitherBadRequest {
      for
        problemSlug <- pathParams.require("problemSlug").flatMap(ProblemSlug.parse)
        pageRequest <- PageRequestQuerySupport.parsePageRequest(request.uri.query.params)
      yield ProblemBlogsInput(problemSlug, pageRequest)
    }

  /** 管理员读取待审列表；非管理员返回空分页，避免暴露审核队列。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, input: ProblemBlogsInput): IO[BlogListResponse] =
    val normalizedPageRequest = input.pageRequest.normalized
    /** 注意：非题目目录管理员返回空分页而非 403，用于避免暴露某个题目的待审博客队列是否存在。 */
    if !canManageProblemCatalog(actor) then
      IO.pure(PageResponse[BlogSummary](Nil, normalizedPageRequest.page, normalizedPageRequest.pageSize, 0L))
    else
      BlogProblemLinkQueryTable.listPendingByProblem(connection, input.problemSlug, actor.username, normalizedPageRequest)

  /** 判断调用者是否拥有查看题目博客提交队列的权限。 */
  private def canManageProblemCatalog(actor: AuthenticatedUser): Boolean =
    actor.problemManager
