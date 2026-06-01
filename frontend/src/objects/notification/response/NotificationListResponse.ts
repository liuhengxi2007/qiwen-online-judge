import type { NotificationSummary } from '@/objects/notification/response/NotificationSummary'
import { fromNotificationSummaryContract } from '@/objects/notification/response/NotificationSummary'
import { readArray, readNonNegativeSafeInteger, readPositiveSafeInteger, readRecord } from '@/objects/shared/PageResponse'

export type NotificationListResponse = {
  notifications: NotificationSummary[]
  unreadCount: number
  page: number
  pageSize: number
  totalItems: number
}

export function fromNotificationListResponseContract(
  value: unknown,
  label = 'notification list response',
): NotificationListResponse {
  const response = readRecord(value, label)
  return {
    notifications: readArray(response.notifications, `${label} notifications`, fromNotificationSummaryContract),
    unreadCount: readNonNegativeSafeInteger(response.unreadCount, `${label} unread count`),
    page: readPositiveSafeInteger(response.page, `${label} page`),
    pageSize: readPositiveSafeInteger(response.pageSize, `${label} page size`),
    totalItems: readNonNegativeSafeInteger(response.totalItems, `${label} total items`),
  }
}
