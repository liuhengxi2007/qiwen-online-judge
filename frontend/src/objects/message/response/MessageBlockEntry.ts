import type { UserIdentity } from '@/objects/user/UserIdentity'
import { fromUserIdentityContract } from '@/objects/user/UserIdentity'
import { readRecord, readString } from '@/objects/shared/PageResponse'

export type MessageBlockEntry = {
  user: UserIdentity
  createdAt: string
}

export function fromMessageBlockEntryContract(value: unknown, label: string): MessageBlockEntry {
  const entry = readRecord(value, label)
  return {
    user: fromUserIdentityContract(entry.user, `${label} user`),
    createdAt: readString(entry.createdAt, `${label} created at`),
  }
}
