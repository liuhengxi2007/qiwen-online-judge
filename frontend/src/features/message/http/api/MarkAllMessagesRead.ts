import {
  decodeSuccessResponse,
  postJson,
} from '@/shared/api/http-client'
import type { SuccessResponse } from '@/shared/http/response/SuccessResponse'

export function markAllMessagesRead(): Promise<SuccessResponse> {
  return postJson('/api/messages/read-all', decodeSuccessResponse, {})
}
