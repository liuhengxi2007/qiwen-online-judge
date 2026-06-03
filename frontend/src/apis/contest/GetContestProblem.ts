import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { APIMessage } from '@/system/api/api-message'

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
