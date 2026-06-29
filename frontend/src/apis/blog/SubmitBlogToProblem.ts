import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogId } from '@/objects/blog/BlogId'
import { blogIdValue } from '@/objects/blog/BlogId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { SuccessResponse } from '@/objects/shared/transport/SuccessResponse'

/** 提交博客作为题目关联候选；输入题目 slug 和博客 ID，输出通用成功响应。 */
export class SubmitBlogToProblem implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug, blogId: BlogId) {
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/blog-submissions/${blogIdValue(blogId)}`
  }

  body(): undefined {
    return undefined
  }
}
