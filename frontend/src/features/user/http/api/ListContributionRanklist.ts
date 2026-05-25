import { fromUserRanklistResponseContract } from '@/features/user/http/codec/UserHttpCodecs'
import type { UserRanklistItem } from '@/features/user/model/response/UserRanklistItem'
import { requestJson } from '@/shared/api/http-client'
import type { PageResponse } from '@/shared/model/PageResponse'

export async function listContributionRanklist(page: number): Promise<PageResponse<UserRanklistItem>> {
  return requestJson(`/api/users/ranklist?page=${encodeURIComponent(String(page))}`, fromUserRanklistResponseContract)
}
