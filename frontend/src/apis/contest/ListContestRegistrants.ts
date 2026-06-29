import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ContestRegistrantListResponse } from '@/objects/contest/response/ContestRegistrantListResponse'
import type { PageRequest } from '@/objects/shared/PageRequest'
import type { APIMessage } from '@/system/api/api-message'

/** 查询比赛报名列表；输入比赛 slug 和可选分页参数，输出报名用户分页响应。 */
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
