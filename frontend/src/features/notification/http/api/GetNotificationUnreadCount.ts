import type { NotificationUnreadCountResponse } from '@/features/notification/http/response/NotificationUnreadCountResponse'
import { fromNotificationUnreadCountResponse } from '@/features/notification/http/codec/NotificationHttpCodecs'
import { requestJson } from '@/shared/api/http-client'

export function getNotificationUnreadCount(): Promise<NotificationUnreadCountResponse> {
  return requestJson('/api/notifications/unread-count', fromNotificationUnreadCountResponse)
}
