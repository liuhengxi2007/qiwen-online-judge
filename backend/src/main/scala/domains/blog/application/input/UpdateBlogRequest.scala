package domains.blog.application.input

import domains.blog.model.*

final case class UpdateBlogRequest(
  title: BlogTitle,
  content: BlogContent,
  visibility: BlogVisibility
)
