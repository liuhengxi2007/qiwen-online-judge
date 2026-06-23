package domains.blog.objects.request

import domains.blog.objects.{BlogCommentId, BlogId}

/** 创建博客评论的输入；顶层评论没有 parentCommentId，回复会携带父评论 ID。 */
final case class CreateBlogCommentInput(
  blogId: BlogId,
  parentCommentId: Option[BlogCommentId],
  request: CreateBlogCommentRequest
)
