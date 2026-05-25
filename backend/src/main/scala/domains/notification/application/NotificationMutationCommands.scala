package domains.notification.application



import cats.effect.IO
import domains.auth.model.AuthUser
import domains.user.model.Username
import domains.blog.model.response.BlogCommentNotificationContext
import domains.blog.model.BlogCommentId
import domains.notification.application.NotificationCommandResults.{MarkAllNotificationsReadResult, MarkNotificationReadResult}
import domains.notification.model.{NotificationId, NotificationKind, NotificationPayload}
import domains.notification.table.notification.NotificationTable

import java.sql.Connection

object NotificationMutationCommands:

  private val blogReplyTitleKey = "notifications.blogReply.title"
  private val blogReplyBodyKey = "notifications.blogReply.body"
  private val maxPreviewLength = 160

  def createBlogReplyNotifications(
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

        NotificationTable
          .insert(
            connection = connection,
            recipientUsername = recipientUsername,
            actorUsername = Some(actor.username),
            kind = NotificationKind.BlogReply,
            titleKey = blogReplyTitleKey,
            bodyKey = blogReplyBodyKey,
            payload = payload,
            targetPath = s"/blogs/${context.blogId.value}",
            targetAnchor = Some(s"comment-${context.triggerCommentId.value}")
          )
          .as(acc :+ recipientUsername)
      }
    }

  def markNotificationRead(
    connection: Connection,
    actor: AuthUser,
    notificationId: NotificationId
  ): IO[MarkNotificationReadResult] =
    NotificationTable.markRead(connection, notificationId, actor.username).map {
      case true => MarkNotificationReadResult.Marked
      case false => MarkNotificationReadResult.NotFound
    }

  def markAllNotificationsRead(
    connection: Connection,
    actor: AuthUser
  ): IO[MarkAllNotificationsReadResult] =
    NotificationTable.markAllRead(connection, actor.username).as(MarkAllNotificationsReadResult.Marked)

  private def preview(content: String): String =
    val normalized = content.trim
    if normalized.length <= maxPreviewLength then normalized
    else normalized.take(maxPreviewLength - 1) + "…"
