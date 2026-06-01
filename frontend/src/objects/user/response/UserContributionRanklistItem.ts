import type { UserContribution } from '@/objects/user/UserContribution'
import { fromUserContributionContract } from '@/objects/user/UserContribution'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import { fromUserIdentityContract } from '@/objects/user/UserIdentity'
import { readNumber, readRecord } from '@/objects/shared/PageResponse'

export type UserContributionRanklistItem = {
  user: UserIdentity
  contribution: UserContribution
}

export function fromUserContributionRanklistItemContract(value: unknown, label: string): UserContributionRanklistItem {
  const item = readRecord(value, label)
  return {
    user: fromUserIdentityContract(item.user, `${label} user`),
    contribution: fromUserContributionContract(readNumber(item.contribution, `${label} contribution`), `${label} contribution`),
  }
}
