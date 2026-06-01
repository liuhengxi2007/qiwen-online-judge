import type { EmailAddress } from '@/objects/auth/EmailAddress'
import { fromEmailAddressContract } from '@/objects/auth/EmailAddress'
import { readBoolean, readRecord, readString } from '@/objects/shared/PageResponse'
import type { DisplayName } from '@/objects/user/DisplayName'
import { fromDisplayNameContract } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'
import { fromUsernameContract } from '@/objects/user/Username'

export type AuthAccountListItem = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  siteManager: boolean
  problemManager: boolean
}

export function fromAuthAccountListItemContract(value: unknown, label: string): AuthAccountListItem {
  const item = readRecord(value, label)
  return {
    username: fromUsernameContract(readString(item.username, `${label} username`), `${label} username`),
    displayName: fromDisplayNameContract(readString(item.displayName, `${label} display name`), `${label} display name`),
    email: fromEmailAddressContract(readString(item.email, `${label} email`), `${label} email`),
    siteManager: readBoolean(item.siteManager, `${label} site manager`),
    problemManager: readBoolean(item.problemManager, `${label} problem manager`),
  }
}
