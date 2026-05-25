import type { SessionResponse } from '@/features/auth/http/response/SessionResponse'
import { fromSessionResponseContract } from '@/features/user/http/codec/UserHttpCodecs'
import type { UpdateManagedUserPreferencesRequest } from '@/features/user/http/request/UpdateManagedUserPreferencesRequest'
import type { UpdateOwnPreferencesRequest } from '@/features/user/http/request/UpdateOwnPreferencesRequest'
import type { Username } from '@/features/user/model/Username'
import { usernameValue } from '@/features/user/lib/user-parsers'
import {
  toUpdateManagedUserPreferencesRequestContract,
  toUpdateOwnPreferencesRequestContract,
} from '@/features/user/http/codec/UserHttpCodecs'
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
