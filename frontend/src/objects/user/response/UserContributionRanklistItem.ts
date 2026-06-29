import type { UserContribution } from '@/objects/user/UserContribution'
import type { UserIdentity } from '@/objects/user/UserIdentity'

/** 用户贡献排行条目；只包含公开身份和后端计算的贡献值。 */
export type UserContributionRanklistItem = {
  user: UserIdentity
  contribution: UserContribution
}
