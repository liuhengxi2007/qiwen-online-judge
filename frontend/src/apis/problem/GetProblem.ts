import type { APIMessage } from '@/system/api/api-message'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

/** 获取题目详情；输入公开 slug，输出题目详情，公开访问边界由后端处理。 */
export class GetProblem implements APIMessage<ProblemDetail> {
  declare readonly responseType?: ProblemDetail
  readonly method = 'GET'
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug) {
    this.apiPath = `problems/${problemSlugValue(problemSlug)}`
  }

  body(): undefined {
    return undefined
  }
}
