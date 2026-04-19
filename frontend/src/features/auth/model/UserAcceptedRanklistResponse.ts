import type { UserAcceptedRanklistItem } from '@/features/auth/model/UserAcceptedRanklistItem'

export type UserAcceptedRanklistResponse = {
  items: UserAcceptedRanklistItem[]
  page: number
  pageSize: number
  totalItems: number
}
