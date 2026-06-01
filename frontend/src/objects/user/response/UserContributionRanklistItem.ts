import type { UserContribution } from '@/objects/user/UserContribution'
import type { UserIdentity } from '@/objects/user/UserIdentity'

export type UserContributionRanklistItem = {
  user: UserIdentity
  contribution: UserContribution
}