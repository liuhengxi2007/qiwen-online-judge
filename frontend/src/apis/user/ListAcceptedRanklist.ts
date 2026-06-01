import type { APIMessage } from '@/system/api/api-message'
import type { PageResponse } from '@/objects/shared/PageResponse'
import type { UserAcceptedRanklistItem } from '@/objects/user/response/UserAcceptedRanklistItem'
import { fromPageResponseContract } from '@/objects/shared/PageResponse'
import { fromUserAcceptedRanklistItemContract } from '@/objects/user/response/UserAcceptedRanklistItem'

export class ListAcceptedRanklist implements APIMessage<PageResponse<UserAcceptedRanklistItem>> {
  declare readonly responseType?: PageResponse<UserAcceptedRanklistItem>
  readonly method = 'GET'
  readonly decode = (value: unknown) => fromPageResponseContract(value, 'accepted ranklist response', fromUserAcceptedRanklistItemContract)
  readonly apiPath: string

  constructor(page: number) {
    this.apiPath = `users/ranklists/accepted-problems?page=${encodeURIComponent(String(page))}`
  }

  body(): undefined {
    return undefined
  }
}
