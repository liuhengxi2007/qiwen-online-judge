import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogId } from '@/objects/blog/BlogId'
import { blogIdValue } from '@/objects/blog/BlogId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'

export class LinkBlogToProblem implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug, blogId: BlogId) {
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/blog-links/${blogIdValue(blogId)}`
  }

  body(): undefined {
    return undefined
  }
}
