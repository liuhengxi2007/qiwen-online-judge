import type { SessionResponse } from '@/features/auth/model/response/SessionResponse'
import { fromSessionResponseContract } from '@/features/user/http/codec/UserHttpCodecs'
import type { UpdateManagedUserAccountRequest } from '@/features/user/model/request/UpdateManagedUserAccountRequest'
import type { UpdateOwnAccountRequest } from '@/features/user/model/request/UpdateOwnAccountRequest'
import type { Username } from '@/features/user/model/Username'
import { usernameValue } from '@/features/user/lib/user-parsers'
import {
  toUpdateManagedUserAccountRequestContract,
  toUpdateOwnAccountRequestContract,
} from '@/features/user/http/codec/UserHttpCodecs'
import { postJson } from '@/shared/api/http-client'

export function updateOwnUserAccount(
  username: Username,
  request: UpdateOwnAccountRequest,
): Promise<SessionResponse> {
  return postJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/settings/account`,
    fromSessionResponseContract,
    toUpdateOwnAccountRequestContract(request),
  )
}

export function updateManagedUserAccount(
  username: Username,
  request: UpdateManagedUserAccountRequest,
): Promise<SessionResponse> {
  return postJson(
    `/api/users/${encodeURIComponent(usernameValue(username))}/settings/account`,
    fromSessionResponseContract,
    toUpdateManagedUserAccountRequestContract(request),
  )
}
