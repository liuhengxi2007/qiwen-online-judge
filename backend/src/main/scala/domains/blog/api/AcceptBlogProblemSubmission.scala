package domains.blog.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.blog.table.blog.BlogProblemLinkMutationTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

object AcceptBlogProblemSubmission extends AuthenticatedApi[BlogProblemLinkInput, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/blog-submissions/:blogId/accept")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[BlogProblemLinkInput] =
    val _ = request
    BlogProblemLinkInput.fromPathParams(pathParams)

  override def plan(connection: Connection, actor: AuthenticatedUser, input: BlogProblemLinkInput): IO[SuccessResponse] =
    for
      _ <- ProblemBlogAccess.requireProblemCatalogManager(actor)
      accepted <- BlogProblemLinkMutationTable.acceptProblem(connection, input.problemSlug, input.blogId, actor.username)
      _ <- HttpApiError.ensure(accepted, HttpApiError.notFound(ApiMessages.pendingProblemBlogSubmissionNotFound))
    yield SuccessResponse.fromApiMessage(ApiMessages.problemBlogSubmissionAccepted)
