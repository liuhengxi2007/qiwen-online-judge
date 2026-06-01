import type { EmailAddress } from '@/objects/auth/EmailAddress'
import { fromEmailAddressContract } from '@/objects/auth/EmailAddress'
import type { DisplayName } from '@/objects/user/DisplayName'
import { fromDisplayNameContract } from '@/objects/user/DisplayName'
import type { UserPreferences } from '@/objects/user/UserPreferences'
import { fromUserPreferencesContract } from '@/objects/user/UserPreferences'
import type { Username } from '@/objects/user/Username'
import { fromUsernameContract } from '@/objects/user/Username'
import { readBoolean, readRecord, readString } from '@/objects/shared/PageResponse'

export type UserSettingsResponse = {
  displayName: DisplayName
  username: Username
  email: EmailAddress
  preferences: UserPreferences
  siteManager: boolean
  problemManager: boolean
}

export function fromUserSettingsResponseContract(value: unknown, label = 'user settings response'): UserSettingsResponse {
  const response = readRecord(value, label)
  return {
    displayName: fromDisplayNameContract(readString(response.displayName, `${label} display name`), `${label} display name`),
    username: fromUsernameContract(readString(response.username, `${label} username`), `${label} username`),
    email: fromEmailAddressContract(readString(response.email, `${label} email`), `${label} email`),
    preferences: fromUserPreferencesContract(response.preferences, `${label} preferences`),
    siteManager: readBoolean(response.siteManager, `${label} site manager`),
    problemManager: readBoolean(response.problemManager, `${label} problem manager`),
  }
}
