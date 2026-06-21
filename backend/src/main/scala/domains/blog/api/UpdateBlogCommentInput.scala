package domains.blog.api

import domains.blog.objects.{BlogCommentId, BlogId}
import domains.blog.objects.request.UpdateBlogCommentRequest

private[api] final case class UpdateBlogCommentInput(
  blogId: BlogId,
  commentId: BlogCommentId,
  request: UpdateBlogCommentRequest
)
