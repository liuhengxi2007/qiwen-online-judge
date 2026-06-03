import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ContestRegistrantListResponse } from '@/objects/contest/response/ContestRegistrantListResponse'
import type { PageRequest } from '@/objects/shared/PageRequest'
import type { APIMessage } from '@/system/api/api-message'

export class ListContestRegistrants implements APIMessage<ContestRegistrantListResponse> {
  declare readonly responseType?: ContestRegistrantListResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(contestSlug: ContestSlug, pageRequest?: PageRequest) {
    const params = new URLSearchParams()
    if (pageRequest) {
      params.set('page', String(pageRequest.page))
      params.set('pageSize', String(pageRequest.pageSize))
    }
    this.apiPath = `contests/${contestSlugValue(contestSlug)}/registrants${params.size > 0 ? `?${params.toString()}` : ''}`
  }

  body(): undefined {
    return undefined
  }
}
