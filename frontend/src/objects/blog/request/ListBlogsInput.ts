import type { PageRequest } from '@/objects/shared/PageRequest'
import type { Username } from '@/objects/user/Username'

/** 博客列表查询输入；authorUsername 为空时查询全部可见博客。 */
export type ListBlogsInput = {
  authorUsername: Username | null
  pageRequest: PageRequest
}
