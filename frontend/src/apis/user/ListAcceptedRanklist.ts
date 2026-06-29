import type { APIMessage } from '@/system/api/api-message'
import type { PageResponse } from '@/objects/shared/PageResponse'
import type { UserAcceptedRanklistItem } from '@/objects/user/response/UserAcceptedRanklistItem'

/** 获取按通过题数排序的用户排行榜；输入页码，输出分页排行条目。 */
export class ListAcceptedRanklist implements APIMessage<PageResponse<UserAcceptedRanklistItem>> {
  declare readonly responseType?: PageResponse<UserAcceptedRanklistItem>
  readonly method = 'GET'
  readonly apiPath: string

  constructor(page: number) {
    this.apiPath = `users/ranklists/accepted-problems?page=${encodeURIComponent(String(page))}`
  }

  body(): undefined {
    return undefined
  }
}
