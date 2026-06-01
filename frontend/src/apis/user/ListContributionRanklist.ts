import type { APIMessage } from '@/system/api/api-message'
import type { PageResponse } from '@/objects/shared/PageResponse'
import type { UserContributionRanklistItem } from '@/objects/user/response/UserContributionRanklistItem'

export class ListContributionRanklist implements APIMessage<PageResponse<UserContributionRanklistItem>> {
  declare readonly responseType?: PageResponse<UserContributionRanklistItem>
  readonly method = 'GET'
  readonly apiPath: string

  constructor(page: number) {
    this.apiPath = `users/ranklists/contribution?page=${encodeURIComponent(String(page))}`
  }

  body(): undefined {
    return undefined
  }
}
