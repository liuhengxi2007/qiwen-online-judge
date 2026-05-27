package domains.blog.objects.request

import domains.blog.objects.*

final case class UpdateBlogCommentRequest(
  content: BlogCommentContent
)
