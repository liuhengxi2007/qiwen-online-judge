import type { SessionResponse } from '@/features/auth/model/response/SessionResponse'
import type { UpdateManagedUserAccountRequest } from '@/features/auth/model/request/UpdateManagedUserAccountRequest'
import type { UpdateOwnAccountRequest } from '@/features/auth/model/request/UpdateOwnAccountRequest'
import type { Username } from '@/features/user/model/Username'
import { usernameValue } from '@/features/user/lib/user-parsers'
import {
  fromSessionResponseContract,
  toUpdateManagedUserAccountRequestContract,
  toUpdateOwnAccountRequestContract,
} from '@/features/auth/http/codec/AuthHttpCodecs'
import { postJson } from '@/shared/api/http-client'

export function updateOwnAccount(
  username: Username,
  request: UpdateOwnAccountRequest,
): Promise<SessionResponse> {
  return postJson(
    `/api/auth/accounts/${encodeURIComponent(usernameValue(username))}/settings/account`,
    fromSessionResponseContract,
    toUpdateOwnAccountRequestContract(request),
  )
}

export function updateManagedAccount(
  username: Username,
  request: UpdateManagedUserAccountRequest,
): Promise<SessionResponse> {
  return postJson(
    `/api/auth/accounts/${encodeURIComponent(usernameValue(username))}/settings/account`,
    fromSessionResponseContract,
    toUpdateManagedUserAccountRequestContract(request),
  )
}
