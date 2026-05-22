import type { EmailAddress, PlaintextPassword } from '@/features/auth/model/AuthValues'

export type UpdateManagedUserAccountRequest = {
  email: EmailAddress
  newPassword: PlaintextPassword | null
}
