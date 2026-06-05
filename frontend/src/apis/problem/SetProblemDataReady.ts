import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

type SetProblemDataReadyRequest = {
  ready: boolean
}

export class SetProblemDataReady implements APIWithSessionMessage<ProblemDetail> {
  declare readonly responseType?: ProblemDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: SetProblemDataReadyRequest

  constructor(problemSlug: ProblemSlug, ready: boolean, contestSlug?: ContestSlug) {
    this.apiPath = contestSlug
      ? `contests/${contestSlugValue(contestSlug)}/problems/${problemSlugValue(problemSlug)}/data/ready-state`
      : `problems/${problemSlugValue(problemSlug)}/data/ready-state`
    this.request = { ready }
  }

  body(): SetProblemDataReadyRequest {
    return this.request
  }
}
