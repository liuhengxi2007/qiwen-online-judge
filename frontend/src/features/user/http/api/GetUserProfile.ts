import type {
  UserProfileResponse,
  Username,
} from '@/features/user/domain/user'
import {
  fromUserProfileResponseContract,
  usernameValue,
} from '@/features/user/domain/user'
import { requestJson } from '@/shared/api/http-client'

export async function getUserProfile(username: Username): Promise<UserProfileResponse> {
  return requestJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/profile`,
    fromUserProfileResponseContract,
  )
}
