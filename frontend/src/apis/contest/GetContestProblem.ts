import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { APIMessage } from '@/system/api/api-message'

/** 获取比赛中的题目详情；输入比赛 slug 和题目 slug，输出题目详情。 */
export class GetContestProblem implements APIMessage<ProblemDetail> {
  declare readonly responseType?: ProblemDetail
  readonly method = 'GET'
  readonly apiPath: string

  constructor(contestSlug: ContestSlug, problemSlug: ProblemSlug) {
    this.apiPath = `contests/${contestSlugValue(contestSlug)}/problems/${problemSlugValue(problemSlug)}`
  }

  body(): undefined {
    return undefined
  }
}
