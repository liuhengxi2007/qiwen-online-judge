import type { SessionResponse } from '@/features/auth/domain/auth'
import { fromSessionResponseContract } from '@/features/auth/http/codec'
import { requestJson } from '@/shared/api/http-client'

export async function getSession(): Promise<SessionResponse> {
  return requestJson('/api/auth/session', fromSessionResponseContract)
}
