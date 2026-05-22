import type { SessionResponse } from '@/features/auth/domain/auth'
import { fromSessionResponseContract } from '@/features/auth/http/codec'
import type { Username } from '@/features/user/domain/user'
import { usernameValue } from '@/features/user/domain/user'
import { requestJson } from '@/shared/api/http-client'

export async function getUserSettings(username: Username): Promise<SessionResponse> {
  return requestJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/settings`,
    fromSessionResponseContract,
  )
}
