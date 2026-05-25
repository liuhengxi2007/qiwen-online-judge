package domains.blog.model

import domains.user.model.Username

final case class BlogCommentNotificationAncestor(
  commentId: BlogCommentId,
  authorUsername: Username
)
