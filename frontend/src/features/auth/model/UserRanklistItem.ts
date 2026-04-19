import type { UserContribution } from '@/features/auth/model/UserContribution'
import type { UserIdentity } from '@/features/auth/model/UserIdentity'

export type UserRanklistItem = {
  user: UserIdentity
  contribution: UserContribution
}
