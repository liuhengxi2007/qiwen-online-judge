package domains.blog.model.internal

import domains.blog.model.{BlogCommentId, BlogId, BlogTitle}
import domains.user.model.Username

final case class BlogCommentNotificationContext(
  blogId: BlogId,
  blogTitle: BlogTitle,
  blogAuthorUsername: Username,
  triggerCommentId: BlogCommentId,
  triggerCommentContent: String,
  ancestors: List[BlogCommentNotificationAncestor]
)
