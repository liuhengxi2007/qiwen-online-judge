import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import { fromSessionResponseContract } from '@/apis/auth/codecs/AuthHttpCodecs'
import { requestJson } from '@/system/api/http-client'

export async function getSession(): Promise<SessionResponse> {
  return requestJson('/api/auth/session', fromSessionResponseContract)
}
