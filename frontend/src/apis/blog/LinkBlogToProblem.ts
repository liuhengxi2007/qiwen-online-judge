import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogId } from '@/objects/blog/BlogId'
import { blogIdValue } from '@/objects/blog/BlogId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'

/** 将博客直接关联到题目；输入题目 slug 和博客 ID，输出通用成功响应。 */
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
