import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import { fromSessionResponseContract } from '@/apis/user/codecs/UserHttpCodecs'
import type { UpdateManagedUserPreferencesRequest } from '@/objects/user/request/UpdateManagedUserPreferencesRequest'
import type { UpdateOwnPreferencesRequest } from '@/objects/user/request/UpdateOwnPreferencesRequest'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/user-parsers'
import {
  toUpdateManagedUserPreferencesRequestContract,
  toUpdateOwnPreferencesRequestContract,
} from '@/apis/user/codecs/UserHttpCodecs'
import { postJson } from '@/system/api/http-client'

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
