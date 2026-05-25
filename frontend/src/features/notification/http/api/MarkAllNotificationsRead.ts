import type { SuccessResponse } from '@/shared/model/response/SuccessResponse'
import {
  decodeSuccessResponse,
  postJson,
} from '@/shared/api/http-client'

export function markAllNotificationsRead(): Promise<SuccessResponse> {
  return postJson('/api/notifications/read-all', decodeSuccessResponse, {})
}
