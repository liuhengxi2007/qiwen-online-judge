package domains.blog.objects.request

import domains.blog.objects.*

final case class CreateBlogRequest(
  title: BlogTitle,
  content: BlogContent,
  visibility: BlogVisibility
)
