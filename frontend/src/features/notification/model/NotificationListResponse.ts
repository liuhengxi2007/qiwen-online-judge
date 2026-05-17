import type { NotificationSummary } from '@/features/notification/model/NotificationSummary'

export type NotificationListResponse = {
  notifications: NotificationSummary[]
  unreadCount: number
  page: number
  pageSize: number
  totalItems: number
}
