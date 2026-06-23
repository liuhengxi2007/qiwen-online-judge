package domains.blog.objects.request

import domains.blog.objects.BlogId

/** 博客投票输入；路径参数定位博客，请求体携带投票方向。 */
final case class VoteBlogInput(
  blogId: BlogId,
  request: VoteBlogRequest
)
