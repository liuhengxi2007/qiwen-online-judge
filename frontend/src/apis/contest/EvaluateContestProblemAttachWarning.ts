import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ContestProblemAttachWarningResponse } from '@/objects/contest/response/ContestProblemAttachWarningResponse'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

export class EvaluateContestProblemAttachWarning implements APIWithSessionMessage<ContestProblemAttachWarningResponse> {
  declare readonly responseType?: ContestProblemAttachWarningResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(contestSlug: ContestSlug, problemSlug: ProblemSlug) {
    this.apiPath = `contests/${contestSlugValue(contestSlug)}/problems/${problemSlugValue(problemSlug)}/attach-warning`
  }

  body(): undefined {
    return undefined
  }
}
