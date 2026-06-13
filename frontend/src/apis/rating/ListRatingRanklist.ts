import type { RatingRanklistItem } from '@/objects/rating/response/RatingRanklistItem'
import type { PageRequest } from '@/objects/shared/PageRequest'
import type { PageResponse } from '@/objects/shared/PageResponse'
import type { APIWithSessionMessage } from '@/system/api/api-message'

/** 查询 rating 排行榜；可选分页参数，输出用户 rating 分页条目。 */
export class ListRatingRanklist implements APIWithSessionMessage<PageResponse<RatingRanklistItem>> {
  declare readonly responseType?: PageResponse<RatingRanklistItem>
  readonly method = 'GET'
  readonly apiPath: string

  constructor(pageRequest?: PageRequest) {
    const params = new URLSearchParams()
    if (pageRequest) {
      params.set('page', String(pageRequest.page))
      params.set('pageSize', String(pageRequest.pageSize))
    }
    this.apiPath = `ratings/ranklist${params.size > 0 ? `?${params.toString()}` : ''}`
  }

  body(): undefined {
    return undefined
  }
}
