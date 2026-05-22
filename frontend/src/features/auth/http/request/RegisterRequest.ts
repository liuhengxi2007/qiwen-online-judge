import type { DisplayName, EmailAddress, PlaintextPassword, Username } from '@/features/auth/model/AuthValues'

export type RegisterRequest = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  password: PlaintextPassword
}
