import type { UserIdentity } from './auth'

export type NotificationId = string
export type NotificationKind = 'blog_reply'

export type BlogReplyNotificationPayload = {
  kind: 'blog_reply'
  blogId: number
  blogTitle: string
  triggerCommentId: number
  recipientCommentId: number | null
  contentPreview: string
}

export type NotificationPayload = BlogReplyNotificationPayload

export type NotificationSummary = {
  id: NotificationId
  kind: NotificationKind
  actor: UserIdentity | null
  titleKey: string
  bodyKey: string
  payload: NotificationPayload
  targetPath: string
  targetAnchor: string | null
  isRead: boolean
  createdAt: string
}

export type NotificationListResponse = {
  notifications: NotificationSummary[]
  unreadCount: number
  page: number
  pageSize: number
  totalItems: number
}

export type NotificationUnreadCountResponse = {
  unreadCount: number
}
