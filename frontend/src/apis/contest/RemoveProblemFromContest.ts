import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { ContestDetail } from '@/objects/contest/response/ContestDetail'
import type { APIWithSessionMessage } from '@/system/api/api-message'

/** 从比赛移除题目；输入比赛 slug 和题目 slug，输出更新后的比赛详情。 */
export class RemoveProblemFromContest implements APIWithSessionMessage<ContestDetail> {
  declare readonly responseType?: ContestDetail
  readonly method = 'POST'
  readonly apiPath: string

  constructor(contestSlug: ContestSlug, problemSlug: ProblemSlug) {
    this.apiPath = `contests/${contestSlugValue(contestSlug)}/problems/${problemSlugValue(problemSlug)}/delete`
  }

  body(): undefined {
    return undefined
  }
}
