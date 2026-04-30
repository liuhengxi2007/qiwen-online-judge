import type { SuccessResponse } from '@contracts/shared'
import type { NotificationId, NotificationListResponse, NotificationUnreadCountResponse } from '@/features/notification/domain/notification'
import {
  fromNotificationListResponse,
  fromNotificationUnreadCountResponse,
  notificationIdValue,
} from '@/features/notification/domain/notification'
import { decodeSuccessResponse, postJson, requestJson } from '@/shared/api/http-client'

export function listNotifications(): Promise<NotificationListResponse> {
  return requestJson('/api/notifications', fromNotificationListResponse)
}

export function getNotificationUnreadCount(): Promise<NotificationUnreadCountResponse> {
  return requestJson('/api/notifications/unread-count', fromNotificationUnreadCountResponse)
}

export function markNotificationRead(notificationId: NotificationId): Promise<SuccessResponse> {
  return postJson(`/api/notifications/${encodeURIComponent(notificationIdValue(notificationId))}/read`, decodeSuccessResponse, {})
}

export function markAllNotificationsRead(): Promise<SuccessResponse> {
  return postJson('/api/notifications/read-all', decodeSuccessResponse, {})
}
