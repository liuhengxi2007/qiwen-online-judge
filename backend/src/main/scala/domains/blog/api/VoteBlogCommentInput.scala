package domains.blog.api

import domains.blog.objects.{BlogCommentId, BlogId}
import domains.blog.objects.request.VoteBlogCommentRequest

private[api] final case class VoteBlogCommentInput(
  blogId: BlogId,
  commentId: BlogCommentId,
  request: VoteBlogCommentRequest
)
