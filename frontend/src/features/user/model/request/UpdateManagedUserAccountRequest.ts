import type { EmailAddress } from '@/features/auth/model/EmailAddress'
import type { PlaintextPassword } from '@/features/auth/model/PlaintextPassword'

export type UpdateManagedUserAccountRequest = {
  email: EmailAddress
  newPassword: PlaintextPassword | null
}
