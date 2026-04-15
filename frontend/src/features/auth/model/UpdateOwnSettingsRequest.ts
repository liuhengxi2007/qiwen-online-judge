import type { DisplayName, EmailAddress, PlaintextPassword } from '@/features/auth/model/AuthValues'

export type UpdateOwnSettingsRequest = {
  displayName: DisplayName
  email: EmailAddress
  currentPassword: PlaintextPassword
  newPassword: PlaintextPassword | null
}
