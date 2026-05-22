import type { DisplayName, EmailAddress, Username } from '@/features/auth/model/AuthValues'
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
