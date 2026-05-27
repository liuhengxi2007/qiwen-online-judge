import { fromUserAcceptedRanklistResponseContract } from '@/apis/user/codecs/UserHttpCodecs'
import type { UserAcceptedRanklistItem } from '@/objects/user/response/UserAcceptedRanklistItem'
import { requestJson } from '@/system/api/http-client'
import type { PageResponse } from '@/objects/shared/PageResponse'

export async function listAcceptedRanklist(page: number): Promise<PageResponse<UserAcceptedRanklistItem>> {
  return requestJson(
    `/api/users/ranklist/accepted?page=${encodeURIComponent(String(page))}`,
    fromUserAcceptedRanklistResponseContract,
  )
}
