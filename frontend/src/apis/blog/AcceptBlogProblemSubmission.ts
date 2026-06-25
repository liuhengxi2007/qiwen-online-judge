import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { BlogId } from '@/objects/blog/BlogId'
import { blogIdValue } from '@/objects/blog/BlogId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { SuccessResponse } from '@/objects/shared/transport/SuccessResponse'

/** 接受博客投稿关联到题目；输入题目 slug 和博客 ID，输出通用成功响应。 */
export class AcceptBlogProblemSubmission implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug, blogId: BlogId) {
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/blog-submissions/${blogIdValue(blogId)}/accept`
  }

  body(): undefined {
    return undefined
  }
}
