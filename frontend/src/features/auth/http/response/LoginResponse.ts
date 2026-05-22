import type { SessionResponse } from '@/features/auth/http/response/SessionResponse'

export type LoginResponse = SessionResponse & {
  message: string
}
