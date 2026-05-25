import { fromUserAcceptedRanklistResponseContract } from '@/features/user/http/codec/UserHttpCodecs'
import type { UserAcceptedRanklistItem } from '@/features/user/model/response/UserAcceptedRanklistItem'
import { requestJson } from '@/shared/api/http-client'
import type { PageResponse } from '@/shared/model/PageResponse'

export async function listAcceptedRanklist(page: number): Promise<PageResponse<UserAcceptedRanklistItem>> {
  return requestJson(
    `/api/users/ranklist/accepted?page=${encodeURIComponent(String(page))}`,
    fromUserAcceptedRanklistResponseContract,
  )
}
