import type { Username } from '@/features/message/domain/message'
import { usernameValue } from '@/features/user/domain/user'
import {
  decodeSuccessResponse,
  postJson,
} from '@/shared/api/http-client'
import type { SuccessResponse } from '@/shared/model/SuccessResponse'

export function removeMessageBlock(targetUsername: Username): Promise<SuccessResponse> {
  return postJson(
    `/api/messages/blocks/${encodeURIComponent(usernameValue(targetUsername))}/remove`,
    decodeSuccessResponse,
    {},
  )
}
