package domains.blog.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser


import domains.blog.objects.{BlogCommentId, BlogId}
import domains.blog.objects.request.VoteBlogCommentRequest
import domains.blog.objects.response.BlogDetail
import domains.blog.table.blog.BlogCommentVoteTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 对博客评论投票或取消同向投票的认证 API。 */
object VoteBlogComment extends AuthenticatedApi[VoteBlogCommentInput, BlogDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/blogs/:blogId/comments/:commentId/vote")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[BlogDetail] = summon[Encoder[BlogDetail]]

  /** 从路径解析博客 id 与评论 id，并读取投票请求体。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[VoteBlogCommentInput] =
    for
      blogId <- HttpApiError.fromEitherBadRequest(pathParams.require("blogId").flatMap(BlogId.parse))
      commentId <- HttpApiError.fromEitherBadRequest(pathParams.require("commentId").flatMap(BlogCommentId.parse))
      body <- request.as[VoteBlogCommentRequest]
    yield VoteBlogCommentInput(blogId, commentId, body)

  /** 按当前用户写入、更新或撤销评论投票；评论或博客不可见时返回 404。 */
  override def plan(connection: Connection, actor: AuthenticatedUser, input: VoteBlogCommentInput): IO[BlogDetail] =
    BlogCommentVoteTable.voteComment(connection, input.blogId, input.commentId, actor.username, input.request.vote).flatMap {
      case Some(blog) => IO.pure(blog)
      case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.blogCommentNotFound))
    }
