package domains.blog.objects.request

import domains.blog.objects.{BlogCommentId, BlogId}

/** 博客评论投票输入；路径参数定位评论，请求体携带投票方向。 */
final case class VoteBlogCommentInput(
  blogId: BlogId,
  commentId: BlogCommentId,
  request: VoteBlogCommentRequest
)
