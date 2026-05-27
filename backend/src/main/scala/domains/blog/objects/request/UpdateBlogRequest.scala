package domains.blog.objects.request

import domains.blog.objects.*

final case class UpdateBlogRequest(
  title: BlogTitle,
  content: BlogContent,
  visibility: BlogVisibility
)
