import type { BlogListResponse } from '@/features/blog/domain/blog'
import type { Username } from '@/features/auth/domain/auth'
import { usernameValue } from '@/features/auth/domain/auth'
import { fromBlogListResponseContract } from '@/features/blog/domain/blog'
import { requestJson } from '@/shared/api/http-client'
import type { PageRequest } from '@/shared/model/Pagination'

export async function listBlogs(authorUsername?: Username | null, pageRequest?: PageRequest): Promise<BlogListResponse> {
  const url = new URL('/api/blogs', window.location.origin)
  if (authorUsername) {
    url.searchParams.set('username', usernameValue(authorUsername))
  }
  if (pageRequest) {
    url.searchParams.set('page', String(pageRequest.page))
    url.searchParams.set('pageSize', String(pageRequest.pageSize))
  }

  return requestJson(url.pathname + url.search, fromBlogListResponseContract)
}
