package domains.blog.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser


import domains.blog.objects.BlogId
import domains.blog.table.blog.BlogProblemLinkMutationTable
import domains.problem.api.ResolveProblemReference
import domains.problem.objects.ProblemSlug
import io.circe.Encoder
import org.http4s.{Method, Request, Status}

import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

/** 管理员直接把公开博客关联到题目的认证 API。 */
object LinkBlogToProblem extends AuthenticatedApi[BlogProblemLinkInput, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/blog-links/:blogId")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  /** 从路径解析题目 slug 和博客 id，关联入口不读取请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[BlogProblemLinkInput] =
    val _ = request
    blogProblemLinkInput(pathParams)

  /** 校验题目目录管理权限、题目存在性和博客公开性后建立 accepted 关联。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, input: BlogProblemLinkInput): IO[SuccessResponse] =
    for
      _ <- HttpApiError.ensure(canManageProblemCatalog(actor), HttpApiError.forbidden(ApiMessages.problemBlogLinkManageForbidden))
      problem <- ResolveProblemReference.plan(connection, input.problemSlug)
      /** 注意：题目不存在与公开博客不可关联共用 404，避免暴露任一资源存在性。 */
      _ <- HttpApiError.ensure(problem.problem.nonEmpty, HttpApiError.notFound(ApiMessages.problemOrPublicBlogNotFound))
      linked <- BlogProblemLinkMutationTable.linkProblem(connection, input.problemSlug, input.blogId, actor.username)
      _ <- HttpApiError.ensure(linked, HttpApiError.notFound(ApiMessages.problemOrPublicBlogNotFound))
    yield SuccessResponse.fromApiMessage(ApiMessages.blogLinkedToProblem)

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
