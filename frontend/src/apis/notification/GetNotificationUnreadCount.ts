import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { NotificationUnreadCountResponse } from '@/objects/notification/response/NotificationUnreadCountResponse'
import { fromNotificationUnreadCountResponseContract } from '@/objects/notification/response/NotificationUnreadCountResponse'

export class GetNotificationUnreadCount implements APIWithSessionMessage<NotificationUnreadCountResponse> {
  declare readonly responseType?: NotificationUnreadCountResponse
  readonly method = 'GET'
  readonly decode = fromNotificationUnreadCountResponseContract
  readonly apiPath = 'notifications/unread-count'

  body(): undefined {
    return undefined
  }
}
