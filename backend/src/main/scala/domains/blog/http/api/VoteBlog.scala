package domains.blog.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.blog.http.BlogApiSupport.VoteBlogInput
import domains.blog.http.codec.BlogHttpCodecs.given
import domains.blog.objects.BlogId
import domains.blog.objects.request.VoteBlogRequest
import domains.blog.objects.response.BlogDetail
import domains.blog.table.blog.BlogVoteTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object VoteBlog extends AuthenticatedApi[VoteBlogInput, BlogDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/blogs/:blogId/vote")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[BlogDetail] = summon[Encoder[BlogDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[VoteBlogInput] =
    for
      blogId <- HttpApiError.fromEitherBadRequest(pathParams.require("blogId").flatMap(BlogId.parse))
      body <- request.as[VoteBlogRequest]
    yield VoteBlogInput(blogId, body)

  override def plan(connection: Connection, actor: AuthUser, input: VoteBlogInput): IO[BlogDetail] =
    BlogVoteTable.vote(connection, input.blogId, actor.username, input.request.vote).flatMap {
      case Some(blog) => IO.pure(blog)
      case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.blogNotFound))
    }
