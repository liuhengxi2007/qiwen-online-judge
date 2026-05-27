import { fromUserRanklistResponseContract } from '@/apis/user/codecs/UserHttpCodecs'
import type { UserRanklistItem } from '@/objects/user/response/UserRanklistItem'
import { requestJson } from '@/system/api/http-client'
import type { PageResponse } from '@/objects/shared/PageResponse'

export async function listContributionRanklist(page: number): Promise<PageResponse<UserRanklistItem>> {
  return requestJson(`/api/users/ranklist?page=${encodeURIComponent(String(page))}`, fromUserRanklistResponseContract)
}
