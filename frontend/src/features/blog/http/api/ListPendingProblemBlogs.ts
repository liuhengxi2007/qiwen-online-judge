import type { BlogListResponse } from '@/features/blog/http/response/BlogListResponse'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import { problemSlugValue } from '@/features/problem/lib/problem-parsers'
import { fromBlogListResponseContract } from '@/features/blog/http/codec/BlogHttpCodecs'
import { requestJson } from '@/shared/api/http-client'
import type { PageRequest } from '@/shared/model/PageRequest'

export async function listPendingProblemBlogs(problemSlug: ProblemSlug, pageRequest?: PageRequest): Promise<BlogListResponse> {
  const url = new URL(`/api/problems/${problemSlugValue(problemSlug)}/blog-submissions`, window.location.origin)
  if (pageRequest) {
    url.searchParams.set('page', String(pageRequest.page))
    url.searchParams.set('pageSize', String(pageRequest.pageSize))
  }
  return requestJson(url.pathname + url.search, fromBlogListResponseContract)
}
