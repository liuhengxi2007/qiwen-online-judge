package domains.blog.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.blog.http.BlogApiSupport
import domains.blog.http.BlogApiSupport.BlogProblemLinkInput
import domains.blog.objects.BlogId
import domains.blog.table.blog.BlogProblemLinkMutationTable
import domains.problem.objects.ProblemSlug
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.codec.SharedHttpCodecs.given
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

object SubmitBlogToProblem extends AuthenticatedApi[BlogProblemLinkInput, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/blog-submissions/:blogId")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[BlogProblemLinkInput] =
    val _ = request
    blogProblemLinkInput(pathParams)

  override def plan(connection: Connection, actor: AuthUser, input: BlogProblemLinkInput): IO[SuccessResponse] =
    for
      submitted <- BlogProblemLinkMutationTable.submitProblem(connection, input.problemSlug, input.blogId, actor.username)
      _ <- HttpApiError.ensure(submitted, HttpApiError.notFound(ApiMessages.problemOrOwnedPublicBlogNotFound))
    yield BlogApiSupport.success(ApiMessages.blogSubmittedToProblem)

  private def blogProblemLinkInput(pathParams: PathParams): IO[BlogProblemLinkInput] =
    HttpApiError.fromEitherBadRequest {
      for
        problemSlug <- pathParams.require("problemSlug").flatMap(ProblemSlug.parse)
        blogId <- pathParams.require("blogId").flatMap(BlogId.parse)
      yield BlogProblemLinkInput(problemSlug, blogId)
    }
