import type { Username } from '@/objects/user/Username'
import type { PlaintextPassword } from '@/objects/auth/PlaintextPassword'

export type LoginRequest = {
  username: Username
  password: PlaintextPassword
}
