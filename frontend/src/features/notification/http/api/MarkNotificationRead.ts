import type { SuccessResponse } from '@contracts/shared'
import type { NotificationId } from '@/features/notification/domain/notification'
import { notificationIdValue } from '@/features/notification/domain/notification'
import {
  decodeSuccessResponse,
  postJson,
} from '@/shared/api/http-client'

export function markNotificationRead(notificationId: NotificationId): Promise<SuccessResponse> {
  return postJson(`/api/notifications/${encodeURIComponent(notificationIdValue(notificationId))}/read`, decodeSuccessResponse, {})
}
