package domains.blog.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.AuthUser

import domains.blog.objects.BlogId
import domains.blog.table.blog.BlogPostMutationTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}

import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
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
    yield SuccessResponse.fromApiMessage(ApiMessages.blogDeleted)
