import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogListResponse } from '@/objects/blog/response/BlogListResponse'
import type { PageRequest } from '@/objects/shared/PageRequest'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { fromBlogListResponseContract } from '@/objects/blog/response/BlogListResponse'

export class ListPendingProblemBlogs implements APIWithSessionMessage<BlogListResponse> {
  declare readonly responseType?: BlogListResponse
  readonly method = 'GET'
  readonly decode = fromBlogListResponseContract
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug, pageRequest?: PageRequest) {
    const params = new URLSearchParams()
    if (pageRequest) {
      params.set('page', String(pageRequest.page))
      params.set('pageSize', String(pageRequest.pageSize))
    }
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/blog-submissions${params.size > 0 ? `?${params.toString()}` : ''}`
  }

  body(): undefined {
    return undefined
  }
}
