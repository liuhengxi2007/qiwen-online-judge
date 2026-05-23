import type { UserProfileResponse } from '@/features/user/http/response/UserProfileResponse'
import type { Username } from '@/features/user/model/Username'
import { usernameValue } from '@/features/user/lib/user-parsers'
import { fromUserProfileResponseContract } from '@/features/user/http/codec'
import { requestJson } from '@/shared/api/http-client'

export async function getUserProfile(username: Username): Promise<UserProfileResponse> {
  return requestJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/profile`,
    fromUserProfileResponseContract,
  )
}
