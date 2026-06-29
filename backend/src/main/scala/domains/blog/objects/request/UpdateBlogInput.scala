package domains.blog.objects.request

import domains.blog.objects.BlogId

/** 更新博客的输入；路径参数定位博客，请求体携带更新内容。 */
final case class UpdateBlogInput(
  blogId: BlogId,
  request: UpdateBlogRequest
)
