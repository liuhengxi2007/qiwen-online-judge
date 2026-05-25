import type { Username } from '@/features/user/model/Username'
import { usernameValue } from '@/features/user/lib/user-parsers'
import {
  decodeSuccessResponse,
  postJson,
} from '@/shared/api/http-client'
import type { SuccessResponse } from '@/shared/model/response/SuccessResponse'

export function deleteAccount(username: Username): Promise<SuccessResponse> {
  return postJson(`/api/auth/accounts/${encodeURIComponent(usernameValue(username))}/delete`, decodeSuccessResponse, {})
}
