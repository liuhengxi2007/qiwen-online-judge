import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ContestRanklistResponse } from '@/objects/contest/response/ContestRanklistResponse'
import type { PageRequest } from '@/objects/shared/PageRequest'
import type { APIMessage } from '@/system/api/api-message'

/** 查询比赛排行榜；输入比赛 slug 和可选分页参数，输出排行榜分页响应。 */
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
