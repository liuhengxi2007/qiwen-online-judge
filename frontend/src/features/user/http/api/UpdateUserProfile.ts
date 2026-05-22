import type { SessionResponse } from '@/features/auth/domain/auth'
import { fromSessionResponseContract } from '@/features/auth/domain/auth'
import type {
  UpdateManagedUserProfileRequest,
  UpdateOwnProfileRequest,
  Username,
} from '@/features/user/domain/user'
import {
  toUpdateManagedUserProfileRequestContract,
  toUpdateOwnProfileRequestContract,
  usernameValue,
} from '@/features/user/domain/user'
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
