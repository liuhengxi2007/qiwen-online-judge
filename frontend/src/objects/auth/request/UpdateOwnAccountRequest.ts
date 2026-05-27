import type { EmailAddress } from '@/objects/auth/EmailAddress'
import type { PlaintextPassword } from '@/objects/auth/PlaintextPassword'

export type UpdateOwnAccountRequest = {
  email: EmailAddress
  currentPassword: PlaintextPassword
  newPassword: PlaintextPassword | null
}
