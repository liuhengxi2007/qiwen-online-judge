import type { UserIdentity } from '@/features/auth/model/UserIdentity'

export type UserAcceptedRanklistItem = {
  user: UserIdentity
  acceptedCount: number
}
