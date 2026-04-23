import type { UserIdentity } from '@/features/user/model/UserIdentity'

export type UserAcceptedRanklistItem = {
  user: UserIdentity
  acceptedCount: number
}
