import type { DisplayName, EmailAddress, PlaintextPassword } from '@/features/auth/model/AuthValues'

export type UpdateManagedUserSettingsRequest = {
  displayName: DisplayName
  email: EmailAddress
  newPassword: PlaintextPassword | null
}
