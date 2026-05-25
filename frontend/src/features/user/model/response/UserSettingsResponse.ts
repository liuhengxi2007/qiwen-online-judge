import type { EmailAddress } from '@/features/auth/model/EmailAddress'
import type { DisplayName } from '@/features/user/model/DisplayName'
import type { UserPreferences } from '@/features/user/model/UserPreferences'
import type { Username } from '@/features/user/model/Username'

export type UserSettingsResponse = {
  displayName: DisplayName
  username: Username
  email: EmailAddress
  preferences: UserPreferences
  siteManager: boolean
  problemManager: boolean
}
