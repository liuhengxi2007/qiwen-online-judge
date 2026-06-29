package domains.blog.api

import cats.effect.IO
import domains.auth.objects.internal.AuthenticatedUser
import domains.blog.objects.BlogId
import domains.blog.objects.request.BlogProblemLinkInput
import domains.problem.objects.ProblemSlug
import shared.api.{ApiMessages, HttpApiError, PathParams}

/** 题目博客访问和路径解析辅助；API 对齐例外：这是多个后端博客端点共享的支持代码，不是前端端点。 */
private[api] object ProblemBlogAccess:

  def canManageProblemCatalog(actor: AuthenticatedUser): Boolean =
    actor.problemManager

  def requireProblemCatalogManager(actor: AuthenticatedUser): IO[Unit] =
    HttpApiError.ensure(
      canManageProblemCatalog(actor),
      HttpApiError.forbidden(ApiMessages.problemBlogLinkManageForbidden)
    )

  def decodeProblemBlogLinkInput(pathParams: PathParams): IO[BlogProblemLinkInput] =
    HttpApiError.fromEitherBadRequest {
      for
        problemSlug <- pathParams.require("problemSlug").flatMap(ProblemSlug.parse)
        blogId <- pathParams.require("blogId").flatMap(BlogId.parse)
      yield BlogProblemLinkInput(problemSlug, blogId)
    }
