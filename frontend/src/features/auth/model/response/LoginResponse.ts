import type { DisplayName } from '@/features/user/model/DisplayName'
import type { Username } from '@/features/user/model/Username'
import type { EmailAddress } from '@/features/auth/model/EmailAddress'
import type { UserPreferences } from '@/features/user/model/UserPreferences'

export type LoginResponse = {
  displayName: DisplayName
  username: Username
  email: EmailAddress
  preferences: UserPreferences
  siteManager: boolean
  problemManager: boolean
  message: string
}
