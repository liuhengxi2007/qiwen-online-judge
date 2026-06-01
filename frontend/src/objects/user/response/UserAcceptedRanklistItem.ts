import type { UserIdentity } from '@/objects/user/UserIdentity'
import { fromUserIdentityContract } from '@/objects/user/UserIdentity'
import { readNonNegativeSafeInteger, readRecord } from '@/objects/shared/PageResponse'

export type UserAcceptedRanklistItem = {
  user: UserIdentity
  acceptedCount: number
}

export function fromUserAcceptedRanklistItemContract(value: unknown, label: string): UserAcceptedRanklistItem {
  const item = readRecord(value, label)
  return {
    user: fromUserIdentityContract(item.user, `${label} user`),
    acceptedCount: readNonNegativeSafeInteger(item.acceptedCount, `${label} accepted count`),
  }
}
