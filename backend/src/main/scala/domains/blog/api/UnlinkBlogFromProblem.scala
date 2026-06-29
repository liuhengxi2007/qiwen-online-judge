package domains.blog.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.blog.objects.request.BlogProblemLinkInput
import domains.blog.table.blog.BlogProblemLinkMutationTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.transport.SuccessResponse

import java.sql.Connection

object UnlinkBlogFromProblem extends AuthenticatedApi[BlogProblemLinkInput, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/blog-links/:blogId/unlink")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[BlogProblemLinkInput] =
    val _ = request
    ProblemBlogAccess.decodeProblemBlogLinkInput(pathParams)

  override def plan(connection: Connection, actor: AuthenticatedUser, input: BlogProblemLinkInput): IO[SuccessResponse] =
    for
      _ <- ProblemBlogAccess.requireProblemCatalogManager(actor)
      unlinked <- BlogProblemLinkMutationTable.unlinkProblem(connection, input.problemSlug, input.blogId)
      _ <- HttpApiError.ensure(unlinked, HttpApiError.notFound(ApiMessages.problemBlogLinkNotFound))
    yield SuccessResponse.fromApiMessage(ApiMessages.blogUnlinkedFromProblem)
