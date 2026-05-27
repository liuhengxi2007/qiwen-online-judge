import type { NotificationListResponse } from '@/objects/notification/response/NotificationListResponse'
import { fromNotificationListResponse } from '@/apis/notification/codecs/NotificationHttpCodecs'
import { requestJson } from '@/system/api/http-client'
import type { PageRequest } from '@/objects/shared/PageRequest'

export function listNotifications(pageRequest?: PageRequest): Promise<NotificationListResponse> {
  const url = new URL('/api/notifications', window.location.origin)
  if (pageRequest) {
    url.searchParams.set('page', String(pageRequest.page))
    url.searchParams.set('pageSize', String(pageRequest.pageSize))
  }
  return requestJson(url.pathname + url.search, fromNotificationListResponse)
}
