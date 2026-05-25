import type { SessionResponse } from '@/features/auth/http/response/SessionResponse'
import { fromSessionResponseContract } from '@/features/user/http/codec/UserHttpCodecs'
import type { Username } from '@/features/user/model/Username'
import { usernameValue } from '@/features/user/lib/user-parsers'
import { requestJson } from '@/shared/api/http-client'

export async function getUserSettings(username: Username): Promise<SessionResponse> {
  return requestJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/settings`,
    fromSessionResponseContract,
  )
}
