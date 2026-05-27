package domains.blog.objects.internal

import domains.blog.objects.{BlogCommentId, BlogId, BlogTitle}
import domains.user.objects.Username

final case class BlogCommentNotificationContext(
  blogId: BlogId,
  blogTitle: BlogTitle,
  blogAuthorUsername: Username,
  triggerCommentId: BlogCommentId,
  triggerCommentContent: String,
  ancestors: List[BlogCommentNotificationAncestor]
)
