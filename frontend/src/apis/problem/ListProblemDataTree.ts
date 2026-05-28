import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemDataTreeResponse } from '@/objects/problem/response/ProblemDataTreeResponse'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

export class ListProblemDataTree implements APIWithSessionMessage<ProblemDataTreeResponse> {
  declare readonly responseType?: ProblemDataTreeResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug) {
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/tree`
  }

  body(): undefined {
    return undefined
  }
}
