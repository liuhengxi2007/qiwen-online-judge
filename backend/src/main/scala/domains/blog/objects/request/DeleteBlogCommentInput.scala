package domains.blog.objects.request

import domains.blog.objects.{BlogCommentId, BlogId}

/** 删除博客评论的输入；由博客 ID 和评论 ID 路径参数组成。 */
final case class DeleteBlogCommentInput(
  blogId: BlogId,
  commentId: BlogCommentId
)
