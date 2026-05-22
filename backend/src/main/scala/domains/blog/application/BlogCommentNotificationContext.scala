package domains.blog.application



import domains.auth.model.Username
import domains.blog.model.{BlogCommentId, BlogId, BlogTitle}

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
