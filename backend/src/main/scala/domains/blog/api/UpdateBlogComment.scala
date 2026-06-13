package domains.blog.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser


import domains.blog.objects.{BlogCommentContent, BlogCommentId, BlogId}
import domains.blog.objects.request.UpdateBlogCommentRequest
import domains.blog.objects.response.BlogDetail
import domains.blog.table.blog.BlogCommentTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 更新当前用户博客评论的认证 API，成功后返回更新后的博客详情。 */
object UpdateBlogComment extends AuthenticatedApi[UpdateBlogCommentInput, BlogDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/blogs/:blogId/comments/:commentId")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[BlogDetail] = summon[Encoder[BlogDetail]]

  /** 从路径解析博客 id 与评论 id，并读取评论更新请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[UpdateBlogCommentInput] =
    for
      blogId <- HttpApiError.fromEitherBadRequest(pathParams.require("blogId").flatMap(BlogId.parse))
      commentId <- HttpApiError.fromEitherBadRequest(pathParams.require("commentId").flatMap(BlogCommentId.parse))
      body <- request.as[UpdateBlogCommentRequest]
    yield UpdateBlogCommentInput(blogId, commentId, body)

  /** 校验评论内容后，仅当评论属于当前用户且博客可见时更新。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, input: UpdateBlogCommentInput): IO[BlogDetail] =
    for
      content <- HttpApiError.fromEitherBadRequest(BlogCommentContent.parse(input.request.content.value))
      maybeBlog <- BlogCommentTable.updateComment(connection, input.blogId, input.commentId, actor.username, content)
      blog <- maybeBlog match
        case Some(blog) => IO.pure(blog)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.blogCommentNotFound))
    yield blog
