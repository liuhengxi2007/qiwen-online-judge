import type { SessionResponse } from '@/features/auth/model/response/SessionResponse'
import { fromSessionResponseContract } from '@/features/user/http/codec/UserHttpCodecs'
import type { UpdateManagedUserProfileRequest } from '@/features/user/model/request/UpdateManagedUserProfileRequest'
import type { UpdateOwnProfileRequest } from '@/features/user/model/request/UpdateOwnProfileRequest'
import type { Username } from '@/features/user/model/Username'
import { usernameValue } from '@/features/user/lib/user-parsers'
import {
  toUpdateManagedUserProfileRequestContract,
  toUpdateOwnProfileRequestContract,
} from '@/features/user/http/codec/UserHttpCodecs'
import { postJson } from '@/shared/api/http-client'

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
