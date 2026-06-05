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

object ListPendingProblemBlogs extends AuthenticatedApi[ProblemBlogsInput, BlogListResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/blog-submissions")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[BlogListResponse] = summon[Encoder[BlogListResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemBlogsInput] =
    HttpApiError.fromEitherBadRequest {
      pathParams.require("problemSlug").flatMap(ProblemSlug.parse).map { problemSlug =>
        ProblemBlogsInput(problemSlug, PageRequestQuerySupport.parsePageRequest(request.uri.query.params))
      }
    }

  override def plan(connection: Connection, actor: AuthenticatedUser, input: ProblemBlogsInput): IO[BlogListResponse] =
    val normalizedPageRequest = input.pageRequest.normalized
    if !canManageProblemCatalog(actor) then
      IO.pure(PageResponse[BlogSummary](Nil, normalizedPageRequest.page, normalizedPageRequest.pageSize, 0L))
    else
      BlogProblemLinkQueryTable.listPendingByProblem(connection, input.problemSlug, actor.username, normalizedPageRequest)

  private def canManageProblemCatalog(actor: AuthenticatedUser): Boolean =
    actor.problemManager
