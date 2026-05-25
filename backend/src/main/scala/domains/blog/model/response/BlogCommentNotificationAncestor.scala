package domains.blog.model.response

import domains.blog.model.BlogCommentId
import domains.user.model.Username

final case class BlogCommentNotificationAncestor(
  commentId: BlogCommentId,
  authorUsername: Username
)
