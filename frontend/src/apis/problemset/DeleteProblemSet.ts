import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { problemSetSlugValue } from '@/objects/problemset/ProblemSetSlug'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import { decodeSuccessResponse } from '@/system/api/http-client'

export class DeleteProblemSet implements APIWithSessionMessage<SuccessResponse> {
  declare readonly responseType?: SuccessResponse
  readonly method = 'POST'
  readonly decode = decodeSuccessResponse
  readonly apiPath: string

  constructor(problemSetSlug: ProblemSetSlug) {
    this.apiPath = `problem-sets/${problemSetSlugValue(problemSetSlug)}/delete`
  }

  body(): undefined {
    return undefined
  }
}
