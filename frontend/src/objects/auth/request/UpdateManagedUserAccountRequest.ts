import type { EmailAddress } from '@/objects/auth/EmailAddress'
import type { PlaintextPassword } from '@/objects/auth/PlaintextPassword'

export type UpdateManagedUserAccountRequest = {
  email: EmailAddress
  newPassword: PlaintextPassword | null
}
