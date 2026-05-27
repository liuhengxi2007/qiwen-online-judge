import type { BlogListResponse } from '@/objects/blog/response/BlogListResponse'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { fromBlogListResponseContract } from '@/apis/blog/codecs/BlogHttpCodecs'
import { requestJson } from '@/system/api/http-client'
import type { PageRequest } from '@/objects/shared/PageRequest'

export async function listProblemBlogs(problemSlug: ProblemSlug, pageRequest?: PageRequest): Promise<BlogListResponse> {
  const url = new URL(`/api/problems/${problemSlugValue(problemSlug)}/blogs`, window.location.origin)
  if (pageRequest) {
    url.searchParams.set('page', String(pageRequest.page))
    url.searchParams.set('pageSize', String(pageRequest.pageSize))
  }
  return requestJson(url.pathname + url.search, fromBlogListResponseContract)
}
