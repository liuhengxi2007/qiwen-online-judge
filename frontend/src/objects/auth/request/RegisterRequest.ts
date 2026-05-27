import type { DisplayName } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'
import type { EmailAddress } from '@/objects/auth/EmailAddress'
import type { PlaintextPassword } from '@/objects/auth/PlaintextPassword'

export type RegisterRequest = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  password: PlaintextPassword
}
