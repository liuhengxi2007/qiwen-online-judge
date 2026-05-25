package domains.blog.model.request

import domains.blog.model.*

final case class CreateBlogRequest(
  title: BlogTitle,
  content: BlogContent,
  visibility: BlogVisibility
)
