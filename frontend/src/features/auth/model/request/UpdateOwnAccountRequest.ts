import type { EmailAddress } from '@/features/auth/model/EmailAddress'
import type { PlaintextPassword } from '@/features/auth/model/PlaintextPassword'

export type UpdateOwnAccountRequest = {
  email: EmailAddress
  currentPassword: PlaintextPassword
  newPassword: PlaintextPassword | null
}
