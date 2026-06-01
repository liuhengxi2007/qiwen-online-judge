import type { APIMessage } from '@/system/api/api-message'
import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { problemSetSlugValue } from '@/objects/problemset/ProblemSetSlug'

export class GetProblemSet implements APIMessage<ProblemSetDetail> {
  declare readonly responseType?: ProblemSetDetail
  readonly method = 'GET'
  readonly apiPath: string

  constructor(problemSetSlug: ProblemSetSlug) {
    this.apiPath = `problem-sets/${problemSetSlugValue(problemSetSlug)}`
  }

  body(): undefined {
    return undefined
  }
}
