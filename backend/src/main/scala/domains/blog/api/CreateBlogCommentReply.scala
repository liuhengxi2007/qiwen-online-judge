package domains.blog.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.blog.objects.{BlogCommentContent, BlogCommentId, BlogId}
import domains.blog.objects.request.{CreateBlogCommentInput, CreateBlogCommentRequest}
import domains.blog.objects.response.BlogDetail
import domains.blog.table.blog.BlogCommentTable
import domains.notification.utils.NotificationEventHubContext
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

final class CreateBlogCommentReply(notificationEventHub: NotificationEventHubContext)
    extends AuthenticatedApi[CreateBlogCommentInput, BlogDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/blogs/:blogId/comments/:commentId/replies")
  override val successStatus: Status = Status.Created
  override protected val outputEncoder: Encoder[BlogDetail] = summon[Encoder[BlogDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[CreateBlogCommentInput] =
    for
      blogId <- HttpApiError.fromEitherBadRequest(pathParams.require("blogId").flatMap(BlogId.parse))
      commentId <- HttpApiError.fromEitherBadRequest(pathParams.require("commentId").flatMap(BlogCommentId.parse))
      body <- request.as[CreateBlogCommentRequest]
    yield CreateBlogCommentInput(blogId, Some(commentId), body)

  override def plan(connection: Connection, actor: AuthenticatedUser, input: CreateBlogCommentInput): IO[BlogDetail] =
    for
      content <- HttpApiError.fromEitherBadRequest(BlogCommentContent.parse(input.request.content.value))
      maybeCreated <- BlogCommentTable.insertComment(connection, input.blogId, input.parentCommentId, actor.username, content)
      created <- maybeCreated match
        case Some(created) => IO.pure(created)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.blogNotFound))
      (blog, createdCommentId) = created
      maybeContext <- BlogCommentTable.findCommentNotificationContext(connection, input.blogId, createdCommentId)
      _ <- maybeContext match
        case Some(context) => BlogCommentReplyNotifier.createAndPublish(connection, actor, context, notificationEventHub)
        case None => IO.unit
    yield blog
