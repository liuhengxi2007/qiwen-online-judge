import type { DisplayName } from '@/features/user/model/DisplayName'
import type { Username } from '@/features/user/model/Username'
import type { EmailAddress } from '@/features/auth/model/EmailAddress'
import type { PlaintextPassword } from '@/features/auth/model/PlaintextPassword'

export type RegisterRequest = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  password: PlaintextPassword
}
