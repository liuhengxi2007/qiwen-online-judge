import type { Username } from '@/features/user/model/Username'
import type { PlaintextPassword } from '@/features/auth/model/PlaintextPassword'

export type LoginRequest = {
  username: Username
  password: PlaintextPassword
}
