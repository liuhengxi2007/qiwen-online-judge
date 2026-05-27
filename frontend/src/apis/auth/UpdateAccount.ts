import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { UpdateManagedUserAccountRequest } from '@/objects/auth/request/UpdateManagedUserAccountRequest'
import type { UpdateOwnAccountRequest } from '@/objects/auth/request/UpdateOwnAccountRequest'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'
import {
  fromSessionResponseContract,
  toUpdateManagedUserAccountRequestContract,
  toUpdateOwnAccountRequestContract,
} from '@/apis/auth/codecs/AuthHttpCodecs'
import { postJson } from '@/system/api/http-client'

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
