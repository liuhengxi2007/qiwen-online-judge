package domains.blog.objects.request

import domains.user.objects.Username
import shared.objects.PageRequest

/** 博客列表查询输入；authorUsername 为空时查询全部可见博客。 */
final case class ListBlogsInput(
  authorUsername: Option[Username],
  pageRequest: PageRequest
)
