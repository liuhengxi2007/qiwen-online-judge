import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemDataFileListResponse } from '@/objects/problem/response/ProblemDataFileListResponse'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

export class ListProblemDataFiles implements APIWithSessionMessage<ProblemDataFileListResponse> {
  declare readonly responseType?: ProblemDataFileListResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug) {
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data`
  }

  body(): undefined {
    return undefined
  }
}
