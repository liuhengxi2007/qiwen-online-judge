import type { UserIdentity } from '@/objects/user/UserIdentity'

export type UserAcceptedRanklistItem = {
  user: UserIdentity
  acceptedCount: number
}
