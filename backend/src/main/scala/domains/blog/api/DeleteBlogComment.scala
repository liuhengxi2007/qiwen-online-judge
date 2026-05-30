package domains.blog.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser


import domains.blog.objects.{BlogCommentId, BlogId}
import domains.blog.objects.response.BlogDetail
import domains.blog.table.blog.BlogCommentTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object DeleteBlogComment extends AuthenticatedApi[DeleteBlogCommentInput, BlogDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/blogs/:blogId/comments/:commentId/delete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[BlogDetail] = summon[Encoder[BlogDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[DeleteBlogCommentInput] =
    val _ = request
    HttpApiError.fromEitherBadRequest {
      for
        blogId <- pathParams.require("blogId").flatMap(BlogId.parse)
        commentId <- pathParams.require("commentId").flatMap(BlogCommentId.parse)
      yield DeleteBlogCommentInput(blogId, commentId)
    }

  override def plan(connection: Connection, actor: AuthenticatedUser, input: DeleteBlogCommentInput): IO[BlogDetail] =
    BlogCommentTable.deleteComment(connection, input.blogId, input.commentId, actor.username).flatMap {
      case Some(blog) => IO.pure(blog)
      case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.blogCommentNotFound))
    }
