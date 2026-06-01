import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { AddProblemToProblemSetRequest } from '@/objects/problemset/request/AddProblemToProblemSetRequest'
import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { problemSetSlugValue } from '@/objects/problemset/ProblemSetSlug'

export class AddProblemToProblemSet implements APIWithSessionMessage<ProblemSetDetail> {
  declare readonly responseType?: ProblemSetDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: AddProblemToProblemSetRequest

  constructor(problemSetSlug: ProblemSetSlug, request: AddProblemToProblemSetRequest) {
    this.apiPath = `problem-sets/${problemSetSlugValue(problemSetSlug)}/problems`
    this.request = request
  }

  body(): AddProblemToProblemSetRequest {
    return this.request
  }
}
