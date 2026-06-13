import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { NotificationUnreadCountResponse } from '@/objects/notification/response/NotificationUnreadCountResponse'

/** 获取当前会话未读通知数；无请求体，输出计数响应。 */
export class GetNotificationUnreadCount implements APIWithSessionMessage<NotificationUnreadCountResponse> {
  declare readonly responseType?: NotificationUnreadCountResponse
  readonly method = 'GET'
  readonly apiPath = 'notifications/unread-count'

  body(): undefined {
    return undefined
  }
}
