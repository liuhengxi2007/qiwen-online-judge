import type { UserAcceptedRanklistResponse } from '@/features/user/domain/user'
import { fromUserAcceptedRanklistResponseContract } from '@/features/user/http/codec'
import { requestJson } from '@/shared/api/http-client'

export async function listAcceptedRanklist(page: number): Promise<UserAcceptedRanklistResponse> {
  return requestJson(
    `/api/users/ranklist/accepted?page=${encodeURIComponent(String(page))}`,
    fromUserAcceptedRanklistResponseContract,
  )
}
