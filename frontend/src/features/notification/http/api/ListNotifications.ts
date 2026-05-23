import type { NotificationListResponse } from '@/features/notification/http/response/NotificationListResponse'
import { fromNotificationListResponse } from '@/features/notification/http/codec'
import { requestJson } from '@/shared/api/http-client'
import type { PageRequest } from '@/shared/model/PageRequest'

export function listNotifications(pageRequest?: PageRequest): Promise<NotificationListResponse> {
  const url = new URL('/api/notifications', window.location.origin)
  if (pageRequest) {
    url.searchParams.set('page', String(pageRequest.page))
    url.searchParams.set('pageSize', String(pageRequest.pageSize))
  }
  return requestJson(url.pathname + url.search, fromNotificationListResponse)
}
