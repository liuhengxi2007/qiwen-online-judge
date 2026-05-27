import type { UserProfileResponse } from '@/objects/user/response/UserProfileResponse'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'
import { fromUserProfileResponseContract } from '@/apis/user/codecs/UserHttpCodecs'
import { requestJson } from '@/system/api/http-client'

export async function getUserProfile(username: Username): Promise<UserProfileResponse> {
  return requestJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/profile`,
    fromUserProfileResponseContract,
  )
}
