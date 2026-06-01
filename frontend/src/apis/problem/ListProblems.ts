import type { APIMessage } from '@/system/api/api-message'
import type { ProblemListRequest } from '@/objects/problem/request/ProblemListRequest'
import type { ProblemListResponse } from '@/objects/problem/response/ProblemListResponse'

export class ListProblems implements APIMessage<ProblemListResponse> {
  declare readonly responseType?: ProblemListResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(request: ProblemListRequest) {
    const params = new URLSearchParams()
    if (request.query !== null && request.query.trim()) {
      params.set('q', request.query)
    }
    params.set('page', String(request.pageRequest.page))
    params.set('pageSize', String(request.pageRequest.pageSize))
    this.apiPath = `problems?${params.toString()}`
  }

  body(): undefined {
    return undefined
  }
}
