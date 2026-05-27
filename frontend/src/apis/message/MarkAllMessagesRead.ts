import {
  decodeSuccessResponse,
  postJson,
} from '@/system/api/http-client'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'

export function markAllMessagesRead(): Promise<SuccessResponse> {
  return postJson('/api/messages/read-all', decodeSuccessResponse, {})
}
