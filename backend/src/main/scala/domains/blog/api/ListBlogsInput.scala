package domains.blog.api

import domains.user.objects.Username
import shared.objects.PageRequest

private[api] final case class ListBlogsInput(
  authorUsername: Option[Username],
  pageRequest: PageRequest
)
