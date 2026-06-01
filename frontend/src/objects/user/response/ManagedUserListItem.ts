import type { DisplayName } from '@/objects/user/DisplayName'
import { fromDisplayNameContract } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'
import { fromUsernameContract } from '@/objects/user/Username'
import type { EmailAddress } from '@/objects/auth/EmailAddress'
import { fromEmailAddressContract } from '@/objects/auth/EmailAddress'
import { readBoolean, readRecord, readString } from '@/objects/shared/PageResponse'

export type ManagedUserListItem = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  siteManager: boolean
  problemManager: boolean
}

export function fromManagedUserListItemContract(value: unknown, label: string): ManagedUserListItem {
  const user = readRecord(value, label)
  return {
    username: fromUsernameContract(readString(user.username, `${label} username`), `${label} username`),
    displayName: fromDisplayNameContract(readString(user.displayName, `${label} display name`), `${label} display name`),
    email: fromEmailAddressContract(readString(user.email, `${label} email`), `${label} email`),
    siteManager: readBoolean(user.siteManager, `${label} site manager`),
    problemManager: readBoolean(user.problemManager, `${label} problem manager`),
  }
}
