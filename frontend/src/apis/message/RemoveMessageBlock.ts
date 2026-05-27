import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/user-parsers'
import {
  decodeSuccessResponse,
  postJson,
} from '@/system/api/http-client'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'

export function removeMessageBlock(targetUsername: Username): Promise<SuccessResponse> {
  return postJson(
    `/api/messages/blocks/${encodeURIComponent(usernameValue(targetUsername))}/remove`,
    decodeSuccessResponse,
    {},
  )
}
