package domains.blog.model

import domains.auth.model.Username

final case class BlogCommentNotificationAncestor(
  commentId: BlogCommentId,
  authorUsername: Username
)

final case class BlogCommentNotificationContext(
  blogId: BlogId,
  blogTitle: BlogTitle,
  blogAuthorUsername: Username,
  triggerCommentId: BlogCommentId,
  triggerCommentContent: String,
  ancestors: List[BlogCommentNotificationAncestor]
)
