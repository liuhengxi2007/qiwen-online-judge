import type { APIMessage } from '@/system/api/api-message'
import type { PageResponse } from '@/objects/shared/PageResponse'
import type { UserContributionRanklistItem } from '@/objects/user/response/UserContributionRanklistItem'
import { fromPageResponseContract } from '@/objects/shared/PageResponse'
import { fromUserContributionRanklistItemContract } from '@/objects/user/response/UserContributionRanklistItem'

export class ListContributionRanklist implements APIMessage<PageResponse<UserContributionRanklistItem>> {
  declare readonly responseType?: PageResponse<UserContributionRanklistItem>
  readonly method = 'GET'
  readonly decode = (value: unknown) => fromPageResponseContract(value, 'contribution ranklist response', fromUserContributionRanklistItemContract)
  readonly apiPath: string

  constructor(page: number) {
    this.apiPath = `users/ranklists/contribution?page=${encodeURIComponent(String(page))}`
  }

  body(): undefined {
    return undefined
  }
}
