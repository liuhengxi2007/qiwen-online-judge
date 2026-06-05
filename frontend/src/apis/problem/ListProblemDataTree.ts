import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ProblemDataTreeResponse } from '@/objects/problem/response/ProblemDataTreeResponse'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

export class ListProblemDataTree implements APIWithSessionMessage<ProblemDataTreeResponse> {
  declare readonly responseType?: ProblemDataTreeResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug, contestSlug?: ContestSlug) {
    this.apiPath = contestSlug
      ? `contests/${contestSlugValue(contestSlug)}/problems/${problemSlugValue(problemSlug)}/data/files/tree`
      : `problems/${problemSlugValue(problemSlug)}/data/files/tree`
  }

  body(): undefined {
    return undefined
  }
}
