import type { PageResponse } from '@/shared/model/PageResponse'
import type { UserAcceptedRanklistItem } from '@/features/user/http/response/UserAcceptedRanklistItem'
import type { UserRanklistItem } from '@/features/user/http/response/UserRanklistItem'

export type UserAcceptedRanklistResponse = PageResponse<UserAcceptedRanklistItem>
export type UserRanklistResponse = PageResponse<UserRanklistItem>
