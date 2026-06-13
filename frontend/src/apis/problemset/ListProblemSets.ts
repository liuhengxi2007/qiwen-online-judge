import type { APIMessage } from '@/system/api/api-message'
import type { PageRequest } from '@/objects/shared/PageRequest'
import type { ProblemSetListResponse } from '@/objects/problemset/response/ProblemSetListResponse'

/** 查询题集列表；可选分页参数，输出题集摘要分页响应。 */
export class ListProblemSets implements APIMessage<ProblemSetListResponse> {
  declare readonly responseType?: ProblemSetListResponse
  readonly method = 'GET'
  readonly apiPath: string

  constructor(pageRequest?: PageRequest) {
    const params = new URLSearchParams()
    if (pageRequest) {
      params.set('page', String(pageRequest.page))
      params.set('pageSize', String(pageRequest.pageSize))
    }
    this.apiPath = `problem-sets${params.size > 0 ? `?${params.toString()}` : ''}`
  }

  body(): undefined {
    return undefined
  }
}
