package domains.blog.api

import cats.effect.IO
import domains.auth.objects.internal.AuthenticatedUser
import domains.blog.objects.{BlogCommentId, BlogId, BlogTitle}
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
    blogId: BlogId,
    triggerCommentId: BlogCommentId,
    blogTitle: BlogTitle,
    blogAuthorUsername: Username,
    triggerCommentContent: String,
    ancestors: List[(BlogCommentId, Username)],
    notificationEventHub: NotificationEventHubContext
  ): IO[Unit] =
    for
      recipients <- createNotifications(
        connection,
        actor,
        blogId,
        triggerCommentId,
        blogTitle,
        blogAuthorUsername,
        triggerCommentContent,
        ancestors
      )
      _ <- recipients.foldLeft(IO.unit)((acc, username) =>
        acc *> NotificationEventHub.publish(notificationEventHub, username, NotificationStreamEvent.NotificationsChanged)
      )
    yield ()

  private def createNotifications(
    connection: Connection,
    actor: AuthenticatedUser,
    blogId: BlogId,
    triggerCommentId: BlogCommentId,
    blogTitle: BlogTitle,
    blogAuthorUsername: Username,
    triggerCommentContent: String,
    ancestors: List[(BlogCommentId, Username)]
  ): IO[List[Username]] =
    deduplicatedRecipients(actor, blogAuthorUsername, ancestors).foldLeft(IO.pure(List.empty[Username])) {
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
                payload = notificationPayload(
                  blogId,
                  triggerCommentId,
                  blogTitle,
                  triggerCommentContent,
                  recipientCommentId
                ),
                targetPath = s"/blogs/${blogId.value}",
                targetAnchor = Some(s"comment-${triggerCommentId.value}")
              )
            )
            .as(acc :+ recipientUsername)
        }
    }

  private def deduplicatedRecipients(
    actor: AuthenticatedUser,
    blogAuthorUsername: Username,
    ancestors: List[(BlogCommentId, Username)]
  ): List[(Username, Option[BlogCommentId])] =
    val candidates =
      ancestors.map { case (commentId, authorUsername) => authorUsername -> Some(commentId) } :+
        (blogAuthorUsername -> None)

    candidates
      .foldLeft((Set.empty[Username], List.empty[(Username, Option[BlogCommentId])])) {
        case ((seen, acc), candidate @ (username, _)) =>
          if seen.contains(username) then (seen, acc)
          else (seen + username, acc :+ candidate)
      }
      ._2
      .filterNot { case (username, _) => username == actor.username }

  private def notificationPayload(
    blogId: BlogId,
    triggerCommentId: BlogCommentId,
    blogTitle: BlogTitle,
    triggerCommentContent: String,
    recipientCommentId: Option[BlogCommentId]
  ): NotificationPayload =
    NotificationPayload.BlogReply(
      blogId = blogId,
      blogTitle = blogTitle,
      triggerCommentId = triggerCommentId,
      recipientCommentId = recipientCommentId,
      contentPreview = preview(triggerCommentContent)
    )

  private def preview(content: String): String =
    val normalized = content.trim
    if normalized.length <= maxPreviewLength then normalized
    else normalized.take(maxPreviewLength - 1) + "..."
