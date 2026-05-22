import type { DisplayName, Username } from '@/features/user/model/UserValues'
import type { EmailAddress } from '@/features/auth/model/AuthValues'
import type { UserPreferences } from '@/features/user/model/UserPreferences'

export type RegisterResponse = {
  displayName: DisplayName
  username: Username
  email: EmailAddress
  preferences: UserPreferences
  siteManager: boolean
  problemManager: boolean
  message: string
}
