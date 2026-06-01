import type { DisplayName } from '@/objects/user/DisplayName'
import { fromDisplayNameContract } from '@/objects/user/DisplayName'
import { readRecord, readString } from '@/objects/shared/PageResponse'
import type { Username } from '@/objects/user/Username'
import { fromUsernameContract } from '@/objects/user/Username'

export type UserIdentity = {
  username: Username
  displayName: DisplayName
}

type UserIdentityContract = {
  username: string
  displayName: string
}

export function fromUserIdentityContract(value: unknown, label = 'user identity'): UserIdentity {
  const response = readRecord(value, label) as UserIdentityContract
  return {
    username: fromUsernameContract(readString(response.username, `${label} username`), `${label} username`),
    displayName: fromDisplayNameContract(readString(response.displayName, `${label} display name`), `${label} display name`),
  }
}
