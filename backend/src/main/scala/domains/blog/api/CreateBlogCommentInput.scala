package domains.blog.api

import domains.blog.objects.{BlogCommentId, BlogId}
import domains.blog.objects.request.CreateBlogCommentRequest

private[api] final case class CreateBlogCommentInput(
  blogId: BlogId,
  parentCommentId: Option[BlogCommentId],
  request: CreateBlogCommentRequest
)
