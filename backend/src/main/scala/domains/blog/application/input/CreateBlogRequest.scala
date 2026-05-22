package domains.blog.application.input

import domains.blog.model.*

final case class CreateBlogRequest(
  title: BlogTitle,
  content: BlogContent,
  visibility: BlogVisibility
)
