import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import { fromSessionResponseContract } from '@/apis/user/codecs/UserHttpCodecs'
import type { UpdateManagedUserProfileRequest } from '@/objects/user/request/UpdateManagedUserProfileRequest'
import type { UpdateOwnProfileRequest } from '@/objects/user/request/UpdateOwnProfileRequest'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/user-parsers'
import {
  toUpdateManagedUserProfileRequestContract,
  toUpdateOwnProfileRequestContract,
} from '@/apis/user/codecs/UserHttpCodecs'
import { postJson } from '@/system/api/http-client'

export function updateOwnUserProfile(
  username: Username,
  request: UpdateOwnProfileRequest,
): Promise<SessionResponse> {
  return postJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/settings/profile`,
    fromSessionResponseContract,
    toUpdateOwnProfileRequestContract(request),
  )
}

export function updateManagedUserProfile(
  username: Username,
  request: UpdateManagedUserProfileRequest,
): Promise<SessionResponse> {
  return postJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/settings/profile`,
    fromSessionResponseContract,
    toUpdateManagedUserProfileRequestContract(request),
  )
}
