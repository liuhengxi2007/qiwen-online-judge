import type {
  UserProfileResponse,
  Username,
} from '@/features/user/domain/user'
import { usernameValue } from '@/features/user/domain/user'
import { fromUserProfileResponseContract } from '@/features/user/http/codec'
import { requestJson } from '@/shared/api/http-client'

export async function getUserProfile(username: Username): Promise<UserProfileResponse> {
  return requestJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/profile`,
    fromUserProfileResponseContract,
  )
}
