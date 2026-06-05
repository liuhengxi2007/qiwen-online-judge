import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ProblemDataFileListResponse } from '@/objects/problem/response/ProblemDataFileListResponse'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

export class ListProblemDataFiles implements APIWithSessionMessage<ProblemDataFileListResponse> {
  declare readonly responseType?: ProblemDataFileListResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(problemSlug: ProblemSlug, contestSlug?: ContestSlug) {
    const params = new URLSearchParams()
    if (contestSlug) {
      params.set('contestSlug', contestSlugValue(contestSlug))
    }
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/files${params.size > 0 ? `?${params.toString()}` : ''}`
  }

  body(): undefined {
    return undefined
  }
}
