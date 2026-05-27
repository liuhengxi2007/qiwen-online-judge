import type { BlogListResponse } from '@/objects/blog/response/BlogListResponse'
import { usernameValue } from '@/objects/user/user-parsers'
import type { Username } from '@/objects/user/Username'
import { fromBlogListResponseContract } from '@/apis/blog/codecs/BlogHttpCodecs'
import { requestJson } from '@/system/api/http-client'
import type { PageRequest } from '@/objects/shared/PageRequest'

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
