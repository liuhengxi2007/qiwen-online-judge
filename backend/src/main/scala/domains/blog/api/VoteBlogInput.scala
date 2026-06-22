package domains.blog.api

import domains.blog.objects.BlogId
import domains.blog.objects.request.VoteBlogRequest

private[api] final case class VoteBlogInput(
  blogId: BlogId,
  request: VoteBlogRequest
)
