package domains.blog.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser


import domains.blog.objects.BlogId
import domains.blog.table.blog.BlogProblemLinkMutationTable
import domains.problem.objects.ProblemSlug
import io.circe.Encoder
import org.http4s.{Method, Request, Status}

import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

/** 博客作者向题目提交博客关联申请的认证 API。 */
object SubmitBlogToProblem extends AuthenticatedApi[BlogProblemLinkInput, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/blog-submissions/:blogId")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  /** 从路径解析题目 slug 和博客 id，提交入口不读取请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[BlogProblemLinkInput] =
    val _ = request
    blogProblemLinkInput(pathParams)

  /** 仅允许公开且归当前用户所有的博客提交待审关联，失败时返回隐藏资源 404。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, input: BlogProblemLinkInput): IO[SuccessResponse] =
    for
      submitted <- BlogProblemLinkMutationTable.submitProblem(connection, input.problemSlug, input.blogId, actor.username)
      /** 注意：题目不存在、博客不存在、博客非公开或博客不属于当前用户都统一返回 404，以避免暴露资源或所有权状态。 */
      _ <- HttpApiError.ensure(submitted, HttpApiError.notFound(ApiMessages.problemOrOwnedPublicBlogNotFound))
    yield SuccessResponse.fromApiMessage(ApiMessages.blogSubmittedToProblem)

  /** 解析博客-题目关联路径参数，统一复用 problemSlug/blogId 的领域解析规则。 */
  private def blogProblemLinkInput(pathParams: PathParams): IO[BlogProblemLinkInput] =
    HttpApiError.fromEitherBadRequest {
      for
        problemSlug <- pathParams.require("problemSlug").flatMap(ProblemSlug.parse)
        blogId <- pathParams.require("blogId").flatMap(BlogId.parse)
      yield BlogProblemLinkInput(problemSlug, blogId)
    }
