import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { NotificationListResponse } from '@/objects/notification/response/NotificationListResponse'
import type { PageRequest } from '@/objects/shared/PageRequest'
import { fromNotificationListResponseContract } from '@/objects/notification/response/NotificationListResponse'

export class ListNotifications implements APIWithSessionMessage<NotificationListResponse> {
  declare readonly responseType?: NotificationListResponse
  readonly method = 'GET'
  readonly decode = fromNotificationListResponseContract
  readonly apiPath: string

  constructor(pageRequest?: PageRequest) {
    const params = new URLSearchParams()
    if (pageRequest) {
      params.set('page', String(pageRequest.page))
      params.set('pageSize', String(pageRequest.pageSize))
    }
    this.apiPath = `notifications${params.size > 0 ? `?${params.toString()}` : ''}`
  }

  body(): undefined {
    return undefined
  }
}
