package domains.blog.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.blog.http.BlogApiSupport.ProblemBlogsInput
import domains.blog.http.codec.BlogHttpCodecs.given
import domains.blog.objects.response.BlogListResponse
import domains.blog.table.blog.BlogProblemLinkQueryTable
import domains.problem.objects.ProblemSlug
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.utils.PageRequestQuerySupport
import shared.http.{ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object ListProblemBlogs extends AuthenticatedApi[ProblemBlogsInput, BlogListResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/problems/:problemSlug/blogs")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[BlogListResponse] = summon[Encoder[BlogListResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[ProblemBlogsInput] =
    HttpApiError.fromEitherBadRequest {
      pathParams.require("problemSlug").flatMap(ProblemSlug.parse).map { problemSlug =>
        ProblemBlogsInput(problemSlug, PageRequestQuerySupport.parsePageRequest(request.uri.query.params))
      }
    }

  override def plan(connection: Connection, actor: AuthUser, input: ProblemBlogsInput): IO[BlogListResponse] =
    BlogProblemLinkQueryTable.listByProblem(connection, input.problemSlug, actor.username, input.pageRequest.normalized)
