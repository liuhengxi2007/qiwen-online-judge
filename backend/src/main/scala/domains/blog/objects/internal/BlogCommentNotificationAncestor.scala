package domains.blog.objects.internal

import domains.blog.objects.BlogCommentId
import domains.user.objects.Username

/** 评论通知链路中的祖先评论作者，用于回复通知去重和定位。 */
final case class BlogCommentNotificationAncestor(
  commentId: BlogCommentId,
  authorUsername: Username
)
