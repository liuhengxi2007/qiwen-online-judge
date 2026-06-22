package domains.blog.api

import domains.blog.objects.BlogId
import domains.blog.objects.request.UpdateBlogRequest

private[api] final case class UpdateBlogInput(
  blogId: BlogId,
  request: UpdateBlogRequest
)
