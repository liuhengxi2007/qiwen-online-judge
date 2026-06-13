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

/** 管理员解除博客与题目关联的认证 API。 */
object UnlinkBlogFromProblem extends AuthenticatedApi[BlogProblemLinkInput, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/blog-links/:blogId/unlink")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  /** 从路径解析题目 slug 和博客 id，解除关联入口不读取请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[BlogProblemLinkInput] =
    val _ = request
    blogProblemLinkInput(pathParams)

  /** 校验题目目录管理权限后删除关联；不存在关联时返回 404。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, input: BlogProblemLinkInput): IO[SuccessResponse] =
    for
      _ <- HttpApiError.ensure(canManageProblemCatalog(actor), HttpApiError.forbidden(ApiMessages.problemBlogLinkManageForbidden))
      unlinked <- BlogProblemLinkMutationTable.unlinkProblem(connection, input.problemSlug, input.blogId)
      _ <- HttpApiError.ensure(unlinked, HttpApiError.notFound(ApiMessages.problemBlogLinkNotFound))
    yield SuccessResponse.fromApiMessage(ApiMessages.blogUnlinkedFromProblem)

  /** 解析博客-题目关联路径参数，统一复用 problemSlug/blogId 的领域解析规则。 */
  private def blogProblemLinkInput(pathParams: PathParams): IO[BlogProblemLinkInput] =
    HttpApiError.fromEitherBadRequest {
      for
        problemSlug <- pathParams.require("problemSlug").flatMap(ProblemSlug.parse)
        blogId <- pathParams.require("blogId").flatMap(BlogId.parse)
      yield BlogProblemLinkInput(problemSlug, blogId)
    }

  /** 判断调用者是否拥有管理题目目录和博客关联的权限。 */
  private def canManageProblemCatalog(actor: AuthenticatedUser): Boolean =
    actor.problemManager
