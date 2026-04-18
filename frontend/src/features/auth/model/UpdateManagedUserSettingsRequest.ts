import type { DisplayName, EmailAddress, PlaintextPassword } from '@/features/auth/model/AuthValues'
import type { UserPreferences } from '@/features/auth/model/UserPreferences'

export type UpdateManagedUserSettingsRequest = {
  displayName: DisplayName
  email: EmailAddress
  preferences: UserPreferences
  newPassword: PlaintextPassword | null
}
