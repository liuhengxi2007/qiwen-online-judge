package domains.blog.model.request

import domains.blog.model.*

final case class UpdateBlogCommentRequest(
  content: BlogCommentContent
)
