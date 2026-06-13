import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { NotificationId } from '@/objects/notification/NotificationId'
import type { NotificationKind } from '@/objects/notification/NotificationKind'
import type { NotificationPayload } from '@/objects/notification/NotificationPayload'

/** 通知摘要；包含文案 key、载荷、目标跳转和已读状态。 */
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
