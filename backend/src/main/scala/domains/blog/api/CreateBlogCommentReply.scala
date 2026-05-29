package domains.blog.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.AuthUser


import domains.blog.objects.internal.BlogCommentNotificationContext
import domains.blog.objects.{BlogCommentContent, BlogCommentId, BlogId}
import domains.blog.objects.request.CreateBlogCommentRequest
import domains.blog.objects.response.BlogDetail
import domains.blog.table.blog.BlogCommentTable
import domains.notification.api.CreateNotification
import domains.notification.utils.{NotificationEventHub, NotificationStreamEvent}
import domains.notification.objects.{NotificationKind, NotificationPayload}
import domains.user.objects.Username
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

final class CreateBlogCommentReply(notificationEventHub: NotificationEventHub) extends AuthenticatedApi[CreateBlogCommentInput, BlogDetail]:

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

  override def plan(connection: Connection, actor: AuthUser, input: CreateBlogCommentInput): IO[BlogDetail] =
    for
      content <- HttpApiError.fromEitherBadRequest(BlogCommentContent.parse(input.request.content.value))
      maybeCreated <- BlogCommentTable.insertComment(connection, input.blogId, input.parentCommentId, actor.username, content)
      created <- maybeCreated match
        case Some(created) => IO.pure(created)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.blogNotFound))
      (blog, createdCommentId) = created
      maybeContext <- BlogCommentTable.findCommentNotificationContext(connection, input.blogId, createdCommentId)
      notificationRecipients <- maybeContext match
        case Some(context) => createBlogReplyNotifications(connection, actor, context)
        case None => IO.pure(Nil)
      _ <- notificationRecipients.foldLeft(IO.unit)((acc, username) =>
        acc *> notificationEventHub.publish(username, NotificationStreamEvent.NotificationsChanged)
      )
    yield blog

  private val blogReplyTitleKey = "notifications.blogReply.title"
  private val blogReplyBodyKey = "notifications.blogReply.body"
  private val maxPreviewLength = 160

  private def createBlogReplyNotifications(
    connection: Connection,
    actor: AuthUser,
    context: BlogCommentNotificationContext
  ): IO[List[Username]] =
    val candidates =
      context.ancestors.map(ancestor => ancestor.authorUsername -> Some(ancestor.commentId)) :+
        (context.blogAuthorUsername -> None)

    val deduplicated = candidates.foldLeft((Set.empty[Username], List.empty[(Username, Option[BlogCommentId])])) {
      case ((seen, acc), candidate @ (username, _)) =>
        if seen.contains(username) then (seen, acc)
        else (seen + username, acc :+ candidate)
    }._2.filterNot { case (username, _) =>
      username == actor.username
    }

    deduplicated.foldLeft(IO.pure(List.empty[Username])) { case (accIo, (recipientUsername, recipientCommentId)) =>
      accIo.flatMap { acc =>
        val payload = NotificationPayload.BlogReply(
          blogId = context.blogId,
          blogTitle = context.blogTitle,
          triggerCommentId = context.triggerCommentId,
          recipientCommentId = recipientCommentId,
          contentPreview = preview(context.triggerCommentContent)
        )

        CreateNotification
          .plan(
            connection,
            CreateNotification.request(
              recipientUsername = recipientUsername,
              actorUsername = Some(actor.username),
              kind = NotificationKind.BlogReply,
              titleKey = blogReplyTitleKey,
              bodyKey = blogReplyBodyKey,
              payload = payload,
              targetPath = s"/blogs/${context.blogId.value}",
              targetAnchor = Some(s"comment-${context.triggerCommentId.value}")
            )
          )
          .as(acc :+ recipientUsername)
      }
    }

  private def preview(content: String): String =
    val normalized = content.trim
    if normalized.length <= maxPreviewLength then normalized
    else normalized.take(maxPreviewLength - 1) + "..."
