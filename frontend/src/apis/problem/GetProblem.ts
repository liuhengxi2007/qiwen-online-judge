import type { APIMessage } from '@/system/api/api-message'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { fromProblemDetailContract } from '@/objects/problem/response/ProblemDetail'

export class GetProblem implements APIMessage<ProblemDetail> {
  declare readonly responseType?: ProblemDetail
  readonly method = 'GET'
  readonly decode = fromProblemDetailContract
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug) {
    this.apiPath = `problems/${problemSlugValue(problemSlug)}`
  }

  body(): undefined {
    return undefined
  }
}
