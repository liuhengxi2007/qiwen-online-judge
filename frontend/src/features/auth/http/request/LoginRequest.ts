import type { Username } from '@/features/user/model/UserValues'
import type { PlaintextPassword } from '@/features/auth/model/AuthValues'

export type LoginRequest = {
  username: Username
  password: PlaintextPassword
}
