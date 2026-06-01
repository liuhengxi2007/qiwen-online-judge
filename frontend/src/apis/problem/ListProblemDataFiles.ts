import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemDataFileListResponse } from '@/objects/problem/response/ProblemDataFileListResponse'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import { fromProblemDataFileListResponseContract } from '@/objects/problem/response/ProblemDataFileListResponse'

export class ListProblemDataFiles implements APIWithSessionMessage<ProblemDataFileListResponse> {
  declare readonly responseType?: ProblemDataFileListResponse
  readonly method = 'GET'
  readonly decode = fromProblemDataFileListResponseContract
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug) {
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/files`
  }

  body(): undefined {
    return undefined
  }
}
