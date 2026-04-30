import type { NotificationSummary } from '@/features/notification/model/NotificationSummary'

export type NotificationListResponse = {
  notifications: NotificationSummary[]
  unreadCount: number
}
