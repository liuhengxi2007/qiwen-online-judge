package domains.blog.model.request

import domains.blog.model.*

final case class CreateBlogCommentRequest(
  content: BlogCommentContent
)
