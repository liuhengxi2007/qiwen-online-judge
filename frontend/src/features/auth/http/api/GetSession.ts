import type { SessionResponse } from '@/features/auth/model/response/SessionResponse'
import { fromSessionResponseContract } from '@/features/auth/http/codec/AuthHttpCodecs'
import { requestJson } from '@/shared/api/http-client'

export async function getSession(): Promise<SessionResponse> {
  return requestJson('/api/auth/session', fromSessionResponseContract)
}
