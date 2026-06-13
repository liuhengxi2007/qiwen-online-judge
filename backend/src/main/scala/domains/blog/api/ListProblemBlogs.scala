package domains.blog.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser


import domains.blog.objects.response.BlogListResponse
import domains.blog.table.blog.BlogProblemLinkQueryTable
import domains.problem.objects.ProblemSlug
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 分页列出题目已接受博客关联的认证 API，按博客可见性过滤结果。 */
object ListProblemBlogs extends AuthenticatedApi[ProblemBlogsInput, BlogListResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/blogs")
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

  /** 读取题目的 accepted 博客关联，私有博客只对作者本人可见。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, input: ProblemBlogsInput): IO[BlogListResponse] =
    BlogProblemLinkQueryTable.listByProblem(connection, input.problemSlug, actor.username, input.pageRequest.normalized)
