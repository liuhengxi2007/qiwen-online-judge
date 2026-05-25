import type { Username } from '@/features/user/model/Username'
import { usernameValue } from '@/features/user/lib/user-parsers'
import {
  decodeSuccessResponse,
  postJson,
} from '@/shared/api/http-client'
import type { SuccessResponse } from '@/shared/model/response/SuccessResponse'

export function removeMessageBlock(targetUsername: Username): Promise<SuccessResponse> {
  return postJson(
    `/api/messages/blocks/${encodeURIComponent(usernameValue(targetUsername))}/remove`,
    decodeSuccessResponse,
    {},
  )
}
