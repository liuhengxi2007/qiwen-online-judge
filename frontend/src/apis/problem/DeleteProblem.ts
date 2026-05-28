import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'

export class DeleteProblem implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug) {
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/delete`
  }

  body(): undefined {
    return undefined
  }
}
