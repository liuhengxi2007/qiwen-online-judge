import type { DisplayName } from '@/objects/user/DisplayName'
import { fromDisplayNameContract } from '@/objects/user/DisplayName'
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

export function fromUserIdentityContract(response: UserIdentityContract): UserIdentity {
  return {
    username: fromUsernameContract(response.username, 'user identity username'),
    displayName: fromDisplayNameContract(response.displayName, 'user identity display name'),
  }
}
