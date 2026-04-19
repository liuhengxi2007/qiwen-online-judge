import type { UserRanklistItem } from '@/features/auth/model/UserRanklistItem'

export type UserRanklistResponse = {
  items: UserRanklistItem[]
  page: number
  pageSize: number
  totalItems: number
}
