import type { NotificationSummary } from '@/objects/notification/response/NotificationSummary'

/** 通知列表响应；包含当前页通知、未读总数和分页统计。 */
export type NotificationListResponse = {
  notifications: NotificationSummary[]
  unreadCount: number
  page: number
  pageSize: number
  totalItems: number
}
