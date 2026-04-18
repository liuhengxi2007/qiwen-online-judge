import type { DisplayName, EmailAddress, Username } from '@/features/auth/model/AuthValues'
import type { UserPreferences } from '@/features/auth/model/UserPreferences'

export type RegisterResponse = {
  displayName: DisplayName
  username: Username
  email: EmailAddress
  preferences: UserPreferences
  siteManager: boolean
  problemManager: boolean
  message: string
}
