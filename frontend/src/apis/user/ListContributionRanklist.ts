import type { APIMessage } from '@/system/api/api-message'
import type { PageResponse } from '@/objects/shared/PageResponse'
import type { UserRanklistItem } from '@/objects/user/response/UserRanklistItem'

export class ListContributionRanklist implements APIMessage<PageResponse<UserRanklistItem>> {
  declare readonly responseType?: PageResponse<UserRanklistItem>
  readonly method = 'GET'
  readonly apiPath: string

  constructor(page: number) {
    this.apiPath = `users/ranklist?page=${encodeURIComponent(String(page))}`
  }

  body(): undefined {
    return undefined
  }
}
