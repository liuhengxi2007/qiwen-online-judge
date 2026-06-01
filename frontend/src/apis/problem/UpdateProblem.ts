import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { UpdateProblemRequest } from '@/objects/problem/request/UpdateProblemRequest'

export class UpdateProblem implements APIWithSessionMessage<ProblemDetail> {
  declare readonly responseType?: ProblemDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: UpdateProblemRequest

  constructor(problemSlug: ProblemSlug, request: UpdateProblemRequest) {
    this.apiPath = `problems/${problemSlugValue(problemSlug)}`
    this.request = request
  }

  body(): UpdateProblemRequest {
    return this.request
  }
}
