import type { APIMessage } from '@/system/api/api-message'
import type { BlogListResponse } from '@/objects/blog/response/BlogListResponse'
import type { PageRequest } from '@/objects/shared/PageRequest'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

/** 查询题目已关联博客；输入题目 slug 和可选分页，输出博客摘要分页响应。 */
export class ListProblemBlogs implements APIMessage<BlogListResponse> {
  declare readonly responseType?: BlogListResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug, pageRequest?: PageRequest) {
    const params = new URLSearchParams()
    if (pageRequest) {
      params.set('page', String(pageRequest.page))
      params.set('pageSize', String(pageRequest.pageSize))
    }
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/blogs${params.size > 0 ? `?${params.toString()}` : ''}`
  }

  body(): undefined {
    return undefined
  }
}
