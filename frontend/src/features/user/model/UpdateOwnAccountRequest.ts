import type { EmailAddress, PlaintextPassword } from '@/features/auth/model/AuthValues'

export type UpdateOwnAccountRequest = {
  email: EmailAddress
  currentPassword: PlaintextPassword
  newPassword: PlaintextPassword | null
}
