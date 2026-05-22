import type { PlaintextPassword, Username } from '@/features/auth/model/AuthValues'

export type LoginRequest = {
  username: Username
  password: PlaintextPassword
}
