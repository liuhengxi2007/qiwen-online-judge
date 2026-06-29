import type { ContestListResponse } from '@/objects/contest/response/ContestListResponse'
import type { PageRequest } from '@/objects/shared/PageRequest'
import type { APIMessage } from '@/system/api/api-message'

/** 查询比赛列表；可选分页参数，输出比赛摘要分页响应。 */
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
