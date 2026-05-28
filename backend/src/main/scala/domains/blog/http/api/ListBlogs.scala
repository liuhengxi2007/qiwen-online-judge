package domains.blog.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.blog.http.BlogApiSupport.ListBlogsInput
import domains.blog.http.codec.BlogHttpCodecs.given
import domains.blog.objects.response.BlogListResponse
import domains.blog.table.blog.BlogPostQueryTable
import domains.user.objects.Username
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.utils.PageRequestQuerySupport
import shared.http.{ApiPath, PathParams}

import java.sql.Connection

object ListBlogs extends AuthenticatedApi[ListBlogsInput, BlogListResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/blogs")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[BlogListResponse] = summon[Encoder[BlogListResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[ListBlogsInput] =
    val _ = pathParams
    IO.pure(
      ListBlogsInput(
        authorUsername = request.uri.query.params.get("username").map(Username.canonical),
        pageRequest = PageRequestQuerySupport.parsePageRequest(request.uri.query.params)
      )
    )

  override def plan(connection: Connection, actor: AuthUser, input: ListBlogsInput): IO[BlogListResponse] =
    input.authorUsername match
      case Some(username) =>
        BlogPostQueryTable.listByAuthor(connection, username, actor.username, input.pageRequest.normalized)
      case None =>
        BlogPostQueryTable.listAll(connection, actor.username, input.pageRequest.normalized)
