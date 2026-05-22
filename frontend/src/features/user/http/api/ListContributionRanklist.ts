import type { UserRanklistResponse } from '@/features/user/domain/user'
import { fromUserRanklistResponseContract } from '@/features/user/http/codec'
import { requestJson } from '@/shared/api/http-client'

export async function listContributionRanklist(page: number): Promise<UserRanklistResponse> {
  return requestJson(`/api/users/ranklist?page=${encodeURIComponent(String(page))}`, fromUserRanklistResponseContract)
}
