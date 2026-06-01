import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemDataTreeResponse } from '@/objects/problem/response/ProblemDataTreeResponse'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { fromProblemDataTreeResponseContract } from '@/objects/problem/response/ProblemDataTreeResponse'

export class ListProblemDataTree implements APIWithSessionMessage<ProblemDataTreeResponse> {
  declare readonly responseType?: ProblemDataTreeResponse
  readonly method = 'GET'
  readonly decode = fromProblemDataTreeResponseContract
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug) {
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/files/tree`
  }

  body(): undefined {
    return undefined
  }
}
