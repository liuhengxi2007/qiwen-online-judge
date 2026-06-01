import type { NotificationSummary } from '@/objects/notification/response/NotificationSummary'

export type NotificationListResponse = {
  notifications: NotificationSummary[]
  unreadCount: number
  page: number
  pageSize: number
  totalItems: number
}