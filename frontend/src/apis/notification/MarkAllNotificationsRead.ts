import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import {
  decodeSuccessResponse,
  postJson,
} from '@/system/api/http-client'

export function markAllNotificationsRead(): Promise<SuccessResponse> {
  return postJson('/api/notifications/read-all', decodeSuccessResponse, {})
}
