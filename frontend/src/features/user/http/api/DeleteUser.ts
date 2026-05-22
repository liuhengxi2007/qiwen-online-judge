import type { Username } from '@/features/user/domain/user'
import { usernameValue } from '@/features/user/domain/user'
import {
  decodeSuccessResponse,
  postJson,
} from '@/shared/api/http-client'
import type { SuccessResponse } from '@/shared/model/SuccessResponse'

export function deleteUser(username: Username): Promise<SuccessResponse> {
  return postJson(`/api/users/${encodeURIComponent(usernameValue(username))}/delete`, decodeSuccessResponse, {})
}
