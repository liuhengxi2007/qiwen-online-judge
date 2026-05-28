package domains.blog.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.blog.http.BlogApiSupport
import domains.blog.objects.BlogId
import domains.blog.table.blog.BlogPostMutationTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.codec.SharedHttpCodecs.given
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

object DeleteBlog extends AuthenticatedApi[BlogId, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/blogs/:blogId/delete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[BlogId] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("blogId").flatMap(BlogId.parse))

  override def plan(connection: Connection, actor: AuthUser, blogId: BlogId): IO[SuccessResponse] =
    for
      deleted <- BlogPostMutationTable.delete(connection, blogId, actor.username)
      _ <- HttpApiError.ensure(deleted, HttpApiError.notFound(ApiMessages.blogNotFound))
    yield BlogApiSupport.success(ApiMessages.blogDeleted)
