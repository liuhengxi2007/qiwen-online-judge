import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import type { NotificationId } from '@/objects/notification/NotificationId'
import { notificationIdValue } from '@/objects/notification/NotificationId'
import {
  decodeSuccessResponse,
  postJson,
} from '@/system/api/http-client'

export function markNotificationRead(notificationId: NotificationId): Promise<SuccessResponse> {
  return postJson(`/api/notifications/${encodeURIComponent(notificationIdValue(notificationId))}/read`, decodeSuccessResponse, {})
}
