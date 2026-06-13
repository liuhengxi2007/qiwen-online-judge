package domains.blog.objects.internal

import domains.blog.objects.{BlogCommentId, BlogId, BlogTitle}
import domains.user.objects.Username

/** 创建评论后生成通知所需的上下文，包含博客作者、触发评论和祖先链。 */
final case class BlogCommentNotificationContext(
  blogId: BlogId,
  blogTitle: BlogTitle,
  blogAuthorUsername: Username,
  triggerCommentId: BlogCommentId,
  triggerCommentContent: String,
  ancestors: List[BlogCommentNotificationAncestor]
)
