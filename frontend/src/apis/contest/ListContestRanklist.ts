import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ContestRanklistResponse } from '@/objects/contest/response/ContestRanklistResponse'
import type { PageRequest } from '@/objects/shared/PageRequest'
import type { APIMessage } from '@/system/api/api-message'

export class ListContestRanklist implements APIMessage<ContestRanklistResponse> {
  declare readonly responseType?: ContestRanklistResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(contestSlug: ContestSlug, pageRequest?: PageRequest) {
    const params = new URLSearchParams()
    if (pageRequest) {
      params.set('page', String(pageRequest.page))
      params.set('pageSize', String(pageRequest.pageSize))
    }
    this.apiPath = `contests/${contestSlugValue(contestSlug)}/ranklist${params.size > 0 ? `?${params.toString()}` : ''}`
  }

  body(): undefined {
    return undefined
  }
}
