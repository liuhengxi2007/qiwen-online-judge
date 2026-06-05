import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { UpdateProblemRequest } from '@/objects/problem/request/UpdateProblemRequest'

export class UpdateProblem implements APIWithSessionMessage<ProblemDetail> {
  declare readonly responseType?: ProblemDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: UpdateProblemRequest

  constructor(problemSlug: ProblemSlug, request: UpdateProblemRequest, contestSlug?: ContestSlug) {
    const params = new URLSearchParams()
    if (contestSlug) {
      params.set('contestSlug', contestSlugValue(contestSlug))
    }
    this.apiPath = `problems/${problemSlugValue(problemSlug)}${params.size > 0 ? `?${params.toString()}` : ''}`
    this.request = request
  }

  body(): UpdateProblemRequest {
    return this.request
  }
}
