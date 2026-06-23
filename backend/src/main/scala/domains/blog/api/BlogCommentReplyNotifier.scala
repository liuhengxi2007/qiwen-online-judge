package domains.blog.api

import cats.effect.IO
import domains.auth.objects.internal.AuthenticatedUser
import domains.blog.objects.BlogCommentId
import domains.blog.objects.internal.BlogCommentNotificationContext
import domains.notification.api.CreateNotification
import domains.notification.objects.{NotificationKind, NotificationPayload}
import domains.notification.utils.{NotificationEventHub, NotificationEventHubContext, NotificationStreamEvent}
import domains.user.objects.Username

import java.sql.Connection

/** 博客评论回复通知辅助；API 对齐例外：这是后端通知编排支持代码，不是前端端点。 */
private[api] object BlogCommentReplyNotifier:

  private val blogReplyTitleKey = "notifications.blogReply.title"
  private val blogReplyBodyKey = "notifications.blogReply.body"
  private val maxPreviewLength = 160

  def createAndPublish(
    connection: Connection,
    actor: AuthenticatedUser,
    context: BlogCommentNotificationContext,
    notificationEventHub: NotificationEventHubContext
  ): IO[Unit] =
    for
      recipients <- createNotifications(connection, actor, context)
      _ <- recipients.foldLeft(IO.unit)((acc, username) =>
        acc *> NotificationEventHub.publish(notificationEventHub, username, NotificationStreamEvent.NotificationsChanged)
      )
    yield ()

  private def createNotifications(
    connection: Connection,
    actor: AuthenticatedUser,
    context: BlogCommentNotificationContext
  ): IO[List[Username]] =
    deduplicatedRecipients(actor, context).foldLeft(IO.pure(List.empty[Username])) {
      case (accIo, (recipientUsername, recipientCommentId)) =>
        accIo.flatMap { acc =>
          CreateNotification
            .plan(
              connection,
              CreateNotification.request(
                recipientUsername = recipientUsername,
                actorUsername = Some(actor.username),
                kind = NotificationKind.BlogReply,
                titleKey = blogReplyTitleKey,
                bodyKey = blogReplyBodyKey,
                payload = notificationPayload(context, recipientCommentId),
                targetPath = s"/blogs/${context.blogId.value}",
                targetAnchor = Some(s"comment-${context.triggerCommentId.value}")
              )
            )
            .as(acc :+ recipientUsername)
        }
    }

  private def deduplicatedRecipients(
    actor: AuthenticatedUser,
    context: BlogCommentNotificationContext
  ): List[(Username, Option[BlogCommentId])] =
    val candidates =
      context.ancestors.map(ancestor => ancestor.authorUsername -> Some(ancestor.commentId)) :+
        (context.blogAuthorUsername -> None)

    candidates
      .foldLeft((Set.empty[Username], List.empty[(Username, Option[BlogCommentId])])) {
        case ((seen, acc), candidate @ (username, _)) =>
          if seen.contains(username) then (seen, acc)
          else (seen + username, acc :+ candidate)
      }
      ._2
      .filterNot { case (username, _) => username == actor.username }

  private def notificationPayload(
    context: BlogCommentNotificationContext,
    recipientCommentId: Option[BlogCommentId]
  ): NotificationPayload =
    NotificationPayload.BlogReply(
      blogId = context.blogId,
      blogTitle = context.blogTitle,
      triggerCommentId = context.triggerCommentId,
      recipientCommentId = recipientCommentId,
      contentPreview = preview(context.triggerCommentContent)
    )

  private def preview(content: String): String =
    val normalized = content.trim
    if normalized.length <= maxPreviewLength then normalized
    else normalized.take(maxPreviewLength - 1) + "..."
