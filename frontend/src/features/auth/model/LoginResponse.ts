import type { SessionResponse } from '@/features/auth/model/SessionResponse'

export type LoginResponse = SessionResponse & {
  message: string
}
