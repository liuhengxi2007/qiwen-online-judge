import type { BlogListResponse } from '@/features/blog/http/response/BlogListResponse'
import { usernameValue } from '@/features/user/lib/user-parsers'
import type { Username } from '@/features/user/model/Username'
import { fromBlogListResponseContract } from '@/features/blog/http/codec/BlogHttpCodecs'
import { requestJson } from '@/shared/api/http-client'
import type { PageRequest } from '@/shared/model/PageRequest'

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
