import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'

export class DeleteProblem implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug, contestSlug?: ContestSlug) {
    const params = new URLSearchParams()
    if (contestSlug) {
      params.set('contestSlug', contestSlugValue(contestSlug))
    }
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/delete${params.size > 0 ? `?${params.toString()}` : ''}`
  }

  body(): undefined {
    return undefined
  }
}
