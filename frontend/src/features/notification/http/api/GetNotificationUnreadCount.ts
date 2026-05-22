import type { NotificationUnreadCountResponse } from '@/features/notification/domain/notification'
import { fromNotificationUnreadCountResponse } from '@/features/notification/domain/notification'
import { requestJson } from '@/shared/api/http-client'

export function getNotificationUnreadCount(): Promise<NotificationUnreadCountResponse> {
  return requestJson('/api/notifications/unread-count', fromNotificationUnreadCountResponse)
}
