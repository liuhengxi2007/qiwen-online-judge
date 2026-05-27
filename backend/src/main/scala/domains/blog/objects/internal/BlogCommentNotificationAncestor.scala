package domains.blog.objects.internal

import domains.blog.objects.BlogCommentId
import domains.user.objects.Username

final case class BlogCommentNotificationAncestor(
  commentId: BlogCommentId,
  authorUsername: Username
)
