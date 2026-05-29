import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

export class ClearProblemData implements APIWithSessionMessage<ProblemDetail> {
  declare readonly responseType?: ProblemDetail
  readonly method = 'POST'
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug) {
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/files/delete-all`
  }

  body(): undefined {
    return undefined
  }
}
