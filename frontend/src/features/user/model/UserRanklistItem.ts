import type { UserContribution } from '@/features/user/model/UserContribution'
import type { UserIdentity } from '@/features/user/model/UserIdentity'

export type UserRanklistItem = {
  user: UserIdentity
  contribution: UserContribution
}
