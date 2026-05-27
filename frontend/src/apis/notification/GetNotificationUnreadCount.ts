import type { NotificationUnreadCountResponse } from '@/objects/notification/response/NotificationUnreadCountResponse'
import { fromNotificationUnreadCountResponse } from '@/apis/notification/codecs/NotificationHttpCodecs'
import { requestJson } from '@/system/api/http-client'

export function getNotificationUnreadCount(): Promise<NotificationUnreadCountResponse> {
  return requestJson('/api/notifications/unread-count', fromNotificationUnreadCountResponse)
}
