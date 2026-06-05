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

object LinkBlogToProblem extends AuthenticatedApi[BlogProblemLinkInput, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/blog-links/:blogId")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[BlogProblemLinkInput] =
    val _ = request
    blogProblemLinkInput(pathParams)

  override def plan(connection: Connection, actor: AuthenticatedUser, input: BlogProblemLinkInput): IO[SuccessResponse] =
    for
      _ <- HttpApiError.ensure(canManageProblemCatalog(actor), HttpApiError.forbidden(ApiMessages.problemBlogLinkManageForbidden))
      problem <- ResolveProblemReference.plan(connection, input.problemSlug)
      _ <- HttpApiError.ensure(problem.problem.nonEmpty, HttpApiError.notFound(ApiMessages.problemOrPublicBlogNotFound))
      linked <- BlogProblemLinkMutationTable.linkProblem(connection, input.problemSlug, input.blogId, actor.username)
      _ <- HttpApiError.ensure(linked, HttpApiError.notFound(ApiMessages.problemOrPublicBlogNotFound))
    yield SuccessResponse.fromApiMessage(ApiMessages.blogLinkedToProblem)

  private def blogProblemLinkInput(pathParams: PathParams): IO[BlogProblemLinkInput] =
    HttpApiError.fromEitherBadRequest {
      for
        problemSlug <- pathParams.require("problemSlug").flatMap(ProblemSlug.parse)
        blogId <- pathParams.require("blogId").flatMap(BlogId.parse)
      yield BlogProblemLinkInput(problemSlug, blogId)
    }

  private def canManageProblemCatalog(actor: AuthenticatedUser): Boolean =
    actor.problemManager
