package domains.blog.objects.request

import domains.blog.objects.{BlogCommentId, BlogId}

/** 更新博客评论的输入；路径参数定位评论，请求体携带更新内容。 */
final case class UpdateBlogCommentInput(
  blogId: BlogId,
  commentId: BlogCommentId,
  request: UpdateBlogCommentRequest
)
