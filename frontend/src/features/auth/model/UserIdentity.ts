import type { DisplayName, Username } from '@/features/auth/model/AuthValues'
import type { UserPreferences } from '@/features/auth/model/UserPreferences'

export type UserIdentity = {
  username: Username
  displayName: DisplayName
  preferences: UserPreferences
}
