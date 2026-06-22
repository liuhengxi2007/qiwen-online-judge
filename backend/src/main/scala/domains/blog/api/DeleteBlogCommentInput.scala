package domains.blog.api

import domains.blog.objects.{BlogCommentId, BlogId}

private[api] final case class DeleteBlogCommentInput(
  blogId: BlogId,
  commentId: BlogCommentId
)
