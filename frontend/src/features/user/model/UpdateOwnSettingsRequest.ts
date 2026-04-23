import type { DisplayName, EmailAddress, PlaintextPassword } from '@/features/auth/model/AuthValues'
import type { UserPreferences } from '@/features/user/model/UserPreferences'

export type UpdateOwnSettingsRequest = {
  displayName: DisplayName
  email: EmailAddress
  preferences: UserPreferences
  currentPassword: PlaintextPassword
  newPassword: PlaintextPassword | null
}
