import type { APIWithSessionMessage } from '@/system/api/api-message'
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

  constructor(problemSlug: ProblemSlug, ready: boolean) {
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/ready`
    this.request = { ready }
  }

  body(): SetProblemDataReadyRequest {
    return this.request
  }
}
