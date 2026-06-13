import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { NotificationListResponse } from '@/objects/notification/response/NotificationListResponse'
import type { PageRequest } from '@/objects/shared/PageRequest'

/** 查询当前会话通知列表；可选分页参数，输出通知分页和未读数。 */
export class ListNotifications implements APIWithSessionMessage<NotificationListResponse> {
  declare readonly responseType?: NotificationListResponse
  readonly method = 'GET'
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
