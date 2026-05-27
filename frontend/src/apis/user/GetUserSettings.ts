import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import { fromSessionResponseContract } from '@/apis/user/codecs/UserHttpCodecs'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'
import { requestJson } from '@/system/api/http-client'

export async function getUserSettings(username: Username): Promise<SessionResponse> {
  return requestJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/settings`,
    fromSessionResponseContract,
  )
}
