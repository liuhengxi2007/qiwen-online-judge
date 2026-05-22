import type { SessionResponse } from '@/features/auth/domain/auth'
import { fromSessionResponseContract } from '@/features/auth/domain/auth'
import type {
  UpdateManagedUserAccountRequest,
  UpdateOwnAccountRequest,
  Username,
} from '@/features/user/domain/user'
import {
  toUpdateManagedUserAccountRequestContract,
  toUpdateOwnAccountRequestContract,
  usernameValue,
} from '@/features/user/domain/user'
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
