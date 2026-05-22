import type { SessionResponse } from '@/features/auth/domain/auth'
import { fromSessionResponseContract } from '@/features/auth/http/codec'
import type {
  UpdateManagedUserPreferencesRequest,
  UpdateOwnPreferencesRequest,
  Username,
} from '@/features/user/domain/user'
import { usernameValue } from '@/features/user/domain/user'
import {
  toUpdateManagedUserPreferencesRequestContract,
  toUpdateOwnPreferencesRequestContract,
} from '@/features/user/http/codec'
import { postJson } from '@/shared/api/http-client'

export function updateOwnUserPreferences(
  username: Username,
  request: UpdateOwnPreferencesRequest,
): Promise<SessionResponse> {
  return postJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/settings/preferences`,
    fromSessionResponseContract,
    toUpdateOwnPreferencesRequestContract(request),
  )
}

export function updateManagedUserPreferences(
  username: Username,
  request: UpdateManagedUserPreferencesRequest,
): Promise<SessionResponse> {
  return postJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/settings/preferences`,
    fromSessionResponseContract,
    toUpdateManagedUserPreferencesRequestContract(request),
  )
}
