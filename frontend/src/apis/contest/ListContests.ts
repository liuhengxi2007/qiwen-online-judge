import type { ContestListResponse } from '@/objects/contest/response/ContestListResponse'
import type { PageRequest } from '@/objects/shared/PageRequest'
import type { APIMessage } from '@/system/api/api-message'

export class ListContests implements APIMessage<ContestListResponse> {
  declare readonly responseType?: ContestListResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(pageRequest?: PageRequest) {
    const params = new URLSearchParams()
    if (pageRequest) {
      params.set('page', String(pageRequest.page))
      params.set('pageSize', String(pageRequest.pageSize))
    }
    this.apiPath = `contests${params.size > 0 ? `?${params.toString()}` : ''}`
  }

  body(): undefined {
    return undefined
  }
}
