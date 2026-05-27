package domains.notification.objects



import domains.blog.objects.{BlogCommentId, BlogId, BlogTitle}

sealed trait NotificationPayload

object NotificationPayload:
  val BlogReplyKind = "blog_reply"

  final case class BlogReply(
    blogId: BlogId,
    blogTitle: BlogTitle,
    triggerCommentId: BlogCommentId,
    recipientCommentId: Option[BlogCommentId],
    contentPreview: String
  ) extends NotificationPayload
