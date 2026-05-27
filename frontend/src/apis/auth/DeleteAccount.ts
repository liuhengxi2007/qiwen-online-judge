import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'
import {
  decodeSuccessResponse,
  postJson,
} from '@/system/api/http-client'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'

export function deleteAccount(username: Username): Promise<SuccessResponse> {
  return postJson(`/api/auth/accounts/${encodeURIComponent(usernameValue(username))}/delete`, decodeSuccessResponse, {})
}
