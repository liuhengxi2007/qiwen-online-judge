package domains.blog.model.request

import domains.blog.model.*

final case class UpdateBlogRequest(
  title: BlogTitle,
  content: BlogContent,
  visibility: BlogVisibility
)
