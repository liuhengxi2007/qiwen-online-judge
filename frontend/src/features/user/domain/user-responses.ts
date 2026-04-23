import type { PageResponse } from '@/shared/model/Pagination'
import type { UserAcceptedRanklistItem } from '@/features/user/model/UserAcceptedRanklistItem'
import type { UserRanklistItem } from '@/features/user/model/UserRanklistItem'

export type UserAcceptedRanklistResponse = PageResponse<UserAcceptedRanklistItem>
export type UserRanklistResponse = PageResponse<UserRanklistItem>
